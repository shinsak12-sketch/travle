// 앱 상태: 여행 목록을 폰 안 JSON 파일로 저장
import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/widgets.dart';
import 'package:path_provider/path_provider.dart';
import 'package:timezone/data/latest_10y.dart' as tzdata;

import 'catalog.dart';
import 'checklist_templates.dart';
import 'models.dart';

class Store extends ChangeNotifier {
  Catalog? catalog;
  List<Trip> trips = [];
  bool ready = false;
  File? _file;
  Timer? _saveTimer;

  Future<void> init() async {
    tzdata.initializeTimeZones();
    final results = await Future.wait([Catalog.load(), _openFile()]);
    catalog = results[0] as Catalog;
    final f = results[1] as File;
    _file = f;
    if (await f.exists()) {
      try {
        trips = _decode(await f.readAsString());
      } catch (_) {
        // 깨진 파일은 옆에 보관하고 빈 상태로 시작
        await f.copy('${f.path}.broken');
      }
    }
    ready = true;
    notifyListeners();
  }

  Future<File> _openFile() async {
    final dir = await getApplicationDocumentsDirectory();
    return File('${dir.path}/trips.json');
  }

  List<Trip> _decode(String s) {
    final j = jsonDecode(s) as Map<String, dynamic>;
    return (j['trips'] as List).map((e) => Trip.fromJson(e)).toList();
  }

  String exportJson() => const JsonEncoder.withIndent(
    ' ',
  ).convert({'app': 'travle', 'v': 1, 'saved': DateTime.now().toIso8601String(), 'trips': trips.map((t) => t.toJson()).toList()});

  /// 백업 파일 불러오기 (전체 교체). 끝까지 읽히는 파일일 때만 바꾸고, 아니면 예외
  void importJson(String s) {
    final next = _decode(s);
    for (final t in next) {
      t.fitDays();
    }
    trips = next;
    changed();
  }

  /// 데이터가 바뀌면 화면 갱신 + 0.4초 뒤 저장
  void changed() {
    notifyListeners();
    _saveTimer?.cancel();
    _saveTimer = Timer(const Duration(milliseconds: 400), save);
  }

  Future<void> save() async {
    final f = _file;
    if (f == null) return;
    final tmp = File('${f.path}.tmp');
    await tmp.writeAsString(exportJson(), flush: true);
    await tmp.rename(f.path);
  }

  Trip? byId(String id) => trips.where((t) => t.id == id).firstOrNull;

  List<Trip> get sorted => [...trips]..sort((a, b) => a.start.compareTo(b.start));

  Trip createTrip({required String title, required DateTime start, required List<Leg> legs, int budget = 0}) {
    final t = Trip(title: title, start: start, legs: legs, budget: budget);
    final lat = catalog?.byId[legs.firstOrNull?.destId]?.lat;
    t.climate = guessClimate(lat, start);
    syncChecklist(t);
    trips.add(t);
    changed();
    return t;
  }

  void updateTripBasics(Trip t, {required String title, required DateTime start, required List<Leg> legs, required int budget}) {
    t.title = title;
    t.start = start;
    t.legs = legs;
    t.budget = budget;
    t.fitDays();
    if (!t.climateManual) {
      t.climate = guessClimate(catalog?.byId[legs.firstOrNull?.destId]?.lat, start);
      syncChecklist(t);
    }
    changed();
  }

  void deleteTrip(Trip t) {
    trips.remove(t);
    changed();
  }

  // ---- 일정 카드 ----
  void addItem(Trip t, TripItem item, {int? day}) {
    if (day == null) {
      t.wish.add(item);
    } else {
      t.days[day].add(item);
      _sortDay(t.days[day]);
    }
    _syncExpense(t, item, day);
    changed();
  }

  void updateItem(Trip t, TripItem item) {
    for (final d in t.days) {
      _sortDay(d);
    }
    _syncExpense(t, item, findDay(t, item));
    changed();
  }

  /// 시간이 있는 카드는 시간 순, 없는 카드는 넣은 순서 유지
  void _sortDay(List<TripItem> d) {
    if (d.every((i) => i.time.isNotEmpty)) d.sort((a, b) => a.time.compareTo(b.time));
  }

  int? findDay(Trip t, TripItem item) {
    for (var i = 0; i < t.days.length; i++) {
      if (t.days[i].contains(item)) return i;
    }
    return null;
  }

  void removeItem(Trip t, TripItem item) {
    for (final d in t.days) {
      d.remove(item);
    }
    t.wish.remove(item);
    t.expenses.removeWhere((e) => e.itemId == item.id);
    changed();
  }

  /// day == null 이면 담은 곳으로 되돌림
  void moveItem(Trip t, TripItem item, int? day) => moveTo(t, item, day);

  /// 카드를 다른 날(또는 담은 곳)로 옮김. index를 주면 그 카드 앞에 끼워 넣음
  void moveTo(Trip t, TripItem item, int? day, [int? index]) {
    final fromDay = findDay(t, item);
    final fromIdx = fromDay == null ? -1 : t.days[fromDay].indexOf(item);
    for (final d in t.days) {
      d.remove(item);
    }
    t.wish.remove(item);
    if (day == null) {
      t.wish.add(item);
    } else {
      final list = t.days[day];
      var at = index ?? list.length;
      if (fromDay == day && fromIdx >= 0 && fromIdx < at) at -= 1;
      list.insert(at.clamp(0, list.length), item);
    }
    _syncExpense(t, item, day);
    changed();
  }

  /// to는 이미 뺀 뒤 기준 위치 (onReorderItem)
  void reorder(Trip t, int day, int from, int to) {
    final list = t.days[day];
    final it = list.removeAt(from);
    list.insert(to, it);
    changed();
  }

  void toggleDone(Trip t, TripItem item) {
    item.done = !item.done;
    changed();
  }

  /// 카드에 금액을 적으면 지출 내역에 자동으로 연결
  void _syncExpense(Trip t, TripItem item, int? day) {
    final existing = t.expenses.where((e) => e.itemId == item.id).firstOrNull;
    if (item.cost == null || item.cost == 0) {
      if (existing != null) t.expenses.remove(existing);
      return;
    }
    final date = day == null ? t.start : t.dateOf(day);
    if (existing != null) {
      existing
        ..title = item.title
        ..amount = item.cost!
        ..currency = item.currency
        ..date = date;
    } else {
      t.expenses.add(
        Expense(
          title: item.title,
          category: item.type.expenseCategory,
          amount: item.cost!,
          currency: item.currency,
          date: date,
          itemId: item.id,
        ),
      );
    }
  }

  // ---- 장소 담기 ----
  TripItem? wishedPlace(Trip t, String destId, String name) {
    bool match(TripItem i) => i.type == ItemType.place && i.destId == destId && i.title == name;
    return t.wish.where(match).firstOrNull ?? t.days.expand((d) => d).where(match).firstOrNull;
  }

  void togglePlace(Trip t, Place p) {
    final have = wishedPlace(t, p.destId, p.ko);
    if (have != null) {
      removeItem(t, have);
      return;
    }
    t.wish.add(placeItem(p));
    changed();
  }

  TripItem placeItem(Place p) => TripItem(
    type: ItemType.place,
    title: p.ko,
    destId: p.destId,
    cat: p.cat,
    sub: p.sub,
    lat: p.lat,
    lon: p.lon,
    f: {if (p.en.isNotEmpty) 'en': p.en, if (p.desc.isNotEmpty) 'desc': p.desc},
  );
}

class StoreScope extends InheritedNotifier<Store> {
  const StoreScope({super.key, required Store store, required super.child}) : super(notifier: store);
  static Store of(BuildContext context) => context.dependOnInheritedWidgetOfExactType<StoreScope>()!.notifier!;
}
