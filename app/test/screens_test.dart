// 실제 화면을 그려보는 테스트. --update-goldens 로 돌리면 test/goldens/*.png 로 화면 이미지를 남김
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travle/data/catalog.dart';
import 'package:travle/data/checklist_templates.dart';
import 'package:travle/data/models.dart';
import 'package:travle/data/store.dart';
import 'package:travle/screens/guide_screen.dart';
import 'package:travle/screens/home_screen.dart';
import 'package:travle/screens/item_edit_screen.dart';
import 'package:travle/screens/today_screen.dart';
import 'package:travle/screens/trip_screen.dart';
import 'package:travle/ui/theme.dart';

Future<void> _loadFonts() async {
  Future<void> load(String family, List<String> files) async {
    final l = FontLoader(family);
    for (final f in files) {
      l.addFont(Future.value(ByteData.sublistView(File('assets/fonts/$f').readAsBytesSync())));
    }
    await l.load();
  }

  await load(kSans, ['GothicA1-400.ttf', 'GothicA1-600.ttf', 'GothicA1-800.ttf']);
  await load(kMono, ['SpaceMono-400.ttf', 'SpaceMono-700.ttf']);
  final icons = File('${Platform.environment['FLUTTER_ROOT']}/bin/cache/artifacts/material_fonts/MaterialIcons-Regular.otf');
  if (icons.existsSync()) {
    final l = FontLoader('MaterialIcons')..addFont(Future.value(ByteData.sublistView(icons.readAsBytesSync())));
    await l.load();
  }
}

late Catalog catalog;

Store buildStore() {
  final s = Store()
    ..catalog = catalog
    ..ready = true;
  final now = DateTime.now();
  final today = DateTime(now.year, now.month, now.day);

  // 여행 중인 여행: 오늘이 D2
  final kyoto = Trip(
    title: '교토·오사카',
    start: today.subtract(const Duration(days: 1)),
    legs: [
      Leg(destId: 'JP-OSA', cityKo: '오사카', code: 'OSA', nights: 1),
      Leg(destId: 'JP-KYOTO', cityKo: '교토', code: 'KYO', nights: 3),
    ],
    budget: 1800000,
    rates: {'JPY': 9.2},
  );
  final places = {for (final p in catalog.places['JP-KYOTO']!) p.ko: p};
  TripItem pl(String name, String time) {
    final p = places.entries.firstWhere((e) => e.key.contains(name)).value;
    return s.placeItem(p)..time = time;
  }

  s.trips.add(kyoto);
  s.addItem(kyoto, TripItem(type: ItemType.flight, title: 'KE723', time: '08:10', f: {'from': 'ICN', 'to': 'KIX', 'arrive': '10:05', 'seat': '32A', 'booking': 'QX7K2M'}, cost: 412000, currency: 'KRW'), day: 0);
  s.addItem(kyoto, TripItem(type: ItemType.food, title: '이치란 라멘 도톤보리점', time: '19:00', f: {'menu': '돈코츠 라멘, 반숙 달걀'}, cost: 2400, currency: 'JPY'), day: 0);
  s.addItem(kyoto, pl('후시미', '09:00')..done = true, day: 1);
  s.addItem(kyoto, pl('기요미즈', '11:30'), day: 1);
  s.addItem(kyoto, TripItem(type: ItemType.shopping, title: '니시키 시장', time: '14:00', f: {'buy': '말차 과자, 츠케모노'}), day: 1);
  s.addItem(kyoto, TripItem(type: ItemType.stay, title: '호텔 그레이서리 교토 산조', time: '15:00', f: {'checkout': '11/24 11:00', 'nights': '3', 'booking': '81726354'}, cost: 54000, currency: 'JPY'), day: 1);
  s.addItem(kyoto, TripItem(type: ItemType.memo, title: '기온 산책', time: '18:00', memo: '해 질 무렵 하나미코지, 저녁은 폰토초'), day: 1);
  kyoto.wish.add(pl('금각사', ''));
  kyoto.wish.add(pl('아라시야마', ''));
  kyoto.expenses.add(Expense(title: '이코카 카드 충전', category: '교통', amount: 3000, currency: 'JPY', date: today));
  kyoto.climate = Climate.mild;
  kyoto.activities.add('golf');
  syncChecklist(kyoto);
  for (final c in kyoto.checklist.take(6)) {
    c.done = true;
  }

  s.trips.add(Trip(
    title: '퀸스타운·와나카',
    start: today.add(const Duration(days: 43)),
    legs: [Leg(destId: 'NZ-ZQN', cityKo: '퀸스타운', code: 'ZQN', nights: 6)],
  ));
  s.trips.add(Trip(
    title: '다낭·호이안',
    start: DateTime(now.year, 7, 3).isBefore(today) ? DateTime(now.year, 7, 3) : DateTime(now.year - 1, 7, 3),
    legs: [Leg(destId: 'VN-DAD', cityKo: '다낭', code: 'DAD', nights: 4)],
  ));
  return s;
}

Future<void> shot(WidgetTester tester, Store s, Widget page, String name, {Future<void> Function()? act}) async {
  tester.view.physicalSize = const Size(390 * 3, 844 * 3);
  tester.view.devicePixelRatio = 3;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(StoreScope(
    store: s,
    child: MaterialApp(debugShowCheckedModeBanner: false, theme: buildTheme(), home: page),
  ));
  await tester.pump(const Duration(seconds: 2));
  if (act != null) {
    await act();
    await tester.pump();
    await tester.pump(const Duration(seconds: 2));
  }
  await expectLater(find.byType(MaterialApp), matchesGoldenFile('goldens/$name.png'));
}

void main() {
  setUpAll(() async {
    TestWidgetsFlutterBinding.ensureInitialized();
    await _loadFonts();
    catalog = await Catalog.load();
  });

  test('내장 데이터 로드', () {
    expect(catalog.dests.length, greaterThan(2000));
    expect(catalog.byId['JP-KYOTO']!.noAirport, true);
    expect(catalog.searchCities('교토').first.id, 'JP-KYOTO');
    expect(catalog.searchCities('cebu').first.ko, '세부');
  });

  testWidgets('홈', (t) async => shot(t, buildStore(), const HomeScreen(), '1_home'));

  testWidgets('일정 D1', (t) async {
    final s = buildStore();
    await shot(t, s, TripScreen(tripId: s.trips.first.id), '2_schedule_d1', act: () async {
      await t.tap(find.text('D1'));
    });
  });

  testWidgets('일정 D2', (t) async {
    final s = buildStore();
    await shot(t, s, TripScreen(tripId: s.trips.first.id), '3_schedule_d2');
  });

  testWidgets('체크리스트', (t) async {
    final s = buildStore();
    await shot(t, s, TripScreen(tripId: s.trips.first.id, initialTab: 1), '4_checklist');
  });

  testWidgets('예산', (t) async {
    final s = buildStore();
    await shot(t, s, TripScreen(tripId: s.trips.first.id, initialTab: 2), '5_budget');
  });

  testWidgets('장소 담기', (t) async {
    final s = buildStore();
    await shot(t, s, GuideScreen(tripId: s.trips.first.id, destId: 'JP-KYOTO', day: 1), '6_guide');
  });

  testWidgets('오늘', (t) async {
    final s = buildStore();
    await shot(t, s, TodayScreen(tripId: s.trips.first.id), '7_today');
  });

  testWidgets('항공편 입력', (t) async {
    final s = buildStore();
    await shot(t, s, ItemEditScreen(tripId: s.trips.first.id, newType: ItemType.flight, day: 0), '8_flight_edit');
  });
}
