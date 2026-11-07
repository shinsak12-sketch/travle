// 여행 · 일정 카드 · 지출 · 체크리스트 모델 (JSON 저장)

int _seq = 0;
String newId() => '${DateTime.now().microsecondsSinceEpoch.toRadixString(36)}${(_seq++).toRadixString(36)}';

/// 일정에 넣는 카드 종류
enum ItemType { place, flight, stay, shopping, food, memo }

extension ItemTypeX on ItemType {
  String get label => const {
        ItemType.place: '장소',
        ItemType.flight: '항공편',
        ItemType.stay: '숙소',
        ItemType.shopping: '쇼핑',
        ItemType.food: '맛집',
        ItemType.memo: '메모',
      }[this]!;

  /// 지출 분류 기본값
  String get expenseCategory => const {
        ItemType.place: '관광',
        ItemType.flight: '항공',
        ItemType.stay: '숙소',
        ItemType.shopping: '쇼핑',
        ItemType.food: '식비',
        ItemType.memo: '기타',
      }[this]!;
}

class TripItem {
  String id;
  ItemType type;
  String title;
  String time; // HH:mm, 비어 있으면 시간 없음
  String memo;
  bool done;
  double? cost;
  String currency;

  /// 종류별 추가 정보 (항공편 번호, 출발/도착, 주소, 예약번호, 메뉴, 살 것 등)
  Map<String, String> f;

  /// 내장 장소에서 온 경우: 여행지 id, 분류, 좌표
  String? destId;
  int? cat;
  String sub;
  double? lat;
  double? lon;

  TripItem({
    String? id,
    required this.type,
    required this.title,
    this.time = '',
    this.memo = '',
    this.done = false,
    this.cost,
    this.currency = 'KRW',
    Map<String, String>? f,
    this.destId,
    this.cat,
    this.sub = '',
    this.lat,
    this.lon,
  })  : id = id ?? newId(),
        f = f ?? {};

  String get(String k) => f[k] ?? '';

  Map<String, dynamic> toJson() => {
        'id': id,
        't': type.name,
        'title': title,
        if (time.isNotEmpty) 'time': time,
        if (memo.isNotEmpty) 'memo': memo,
        if (done) 'done': true,
        if (cost != null) 'cost': cost,
        if (cost != null) 'cur': currency,
        if (f.isNotEmpty) 'f': f,
        if (destId != null) 'dest': destId,
        if (cat != null) 'cat': cat,
        if (sub.isNotEmpty) 'sub': sub,
        if (lat != null) 'lat': lat,
        if (lon != null) 'lon': lon,
      };

  factory TripItem.fromJson(Map<String, dynamic> j) => TripItem(
        id: j['id'],
        type: ItemType.values.firstWhere((t) => t.name == j['t'], orElse: () => ItemType.memo),
        title: j['title'] ?? '',
        time: j['time'] ?? '',
        memo: j['memo'] ?? '',
        done: j['done'] ?? false,
        cost: (j['cost'] as num?)?.toDouble(),
        currency: j['cur'] ?? 'KRW',
        f: (j['f'] as Map?)?.map((k, v) => MapEntry('$k', '$v')),
        destId: j['dest'],
        cat: j['cat'],
        sub: j['sub'] ?? '',
        lat: (j['lat'] as num?)?.toDouble(),
        lon: (j['lon'] as num?)?.toDouble(),
      );

  TripItem copy() => TripItem.fromJson(toJson());
}

/// 여행 구간: 도시 하나에 몇 박
class Leg {
  String destId;
  String cityKo;
  String code;
  int nights;
  Leg({required this.destId, required this.cityKo, required this.code, this.nights = 1});

  Map<String, dynamic> toJson() => {'dest': destId, 'ko': cityKo, 'code': code, 'n': nights};
  factory Leg.fromJson(Map<String, dynamic> j) =>
      Leg(destId: j['dest'], cityKo: j['ko'], code: j['code'], nights: j['n'] ?? 1);
}

class Expense {
  String id;
  String title;
  String category;
  double amount;
  String currency;
  DateTime date;
  String? itemId; // 일정 카드에서 자동으로 만든 지출이면 카드 id

  Expense({
    String? id,
    required this.title,
    required this.category,
    required this.amount,
    required this.currency,
    required this.date,
    this.itemId,
  }) : id = id ?? newId();

  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'cat': category,
        'amt': amount,
        'cur': currency,
        'date': date.toIso8601String(),
        if (itemId != null) 'item': itemId,
      };

  factory Expense.fromJson(Map<String, dynamic> j) => Expense(
        id: j['id'],
        title: j['title'] ?? '',
        category: j['cat'] ?? '기타',
        amount: (j['amt'] as num).toDouble(),
        currency: j['cur'] ?? 'KRW',
        date: DateTime.parse(j['date']),
        itemId: j['item'],
      );
}

class CheckItem {
  String id;
  String text;
  String group;
  String src; // 템플릿 키(base, hot, golf…) 또는 custom
  bool done;
  CheckItem({String? id, required this.text, required this.group, required this.src, this.done = false})
      : id = id ?? newId();

  Map<String, dynamic> toJson() => {'id': id, 'text': text, 'g': group, 'src': src, if (done) 'done': true};
  factory CheckItem.fromJson(Map<String, dynamic> j) =>
      CheckItem(id: j['id'], text: j['text'], group: j['g'], src: j['src'], done: j['done'] ?? false);
}

enum Climate { hot, mild, cold }

extension ClimateX on Climate {
  String get label => const {Climate.hot: '더움', Climate.mild: '선선', Climate.cold: '추움'}[this]!;
  String get range => const {Climate.hot: '25°C 이상', Climate.mild: '10–25°C', Climate.cold: '10°C 미만'}[this]!;
}

class Trip {
  String id;
  String title;
  DateTime start;
  List<Leg> legs;
  List<List<TripItem>> days;
  List<TripItem> wish;

  int budget; // 원화 기준 총예산
  Map<String, double> rates; // 1 단위 = 몇 원
  List<Expense> expenses;

  Climate climate;
  bool climateManual;
  bool rainy;
  List<String> activities;
  List<CheckItem> checklist;
  List<String> hiddenChecks; // 사용자가 지운 템플릿 항목 (다시 안 나오게)

  Trip({
    String? id,
    required this.title,
    required this.start,
    required this.legs,
    List<List<TripItem>>? days,
    List<TripItem>? wish,
    this.budget = 0,
    Map<String, double>? rates,
    List<Expense>? expenses,
    this.climate = Climate.mild,
    this.climateManual = false,
    this.rainy = false,
    List<String>? activities,
    List<CheckItem>? checklist,
    List<String>? hiddenChecks,
  })  : id = id ?? newId(),
        days = days ?? [],
        wish = wish ?? [],
        rates = rates ?? {},
        expenses = expenses ?? [],
        activities = activities ?? [],
        checklist = checklist ?? [],
        hiddenChecks = hiddenChecks ?? [] {
    fitDays();
  }

  int get nights => legs.fold(0, (a, l) => a + l.nights);
  int get dayCount => nights + 1;
  DateTime get end => start.add(Duration(days: nights));
  DateTime dateOf(int day) => start.add(Duration(days: day));

  /// 일자별 도시: 구간의 첫날부터 그 구간 마지막 밤 다음 날 오전까지
  Leg? legOf(int day) {
    var d = 0;
    for (final l in legs) {
      if (day < d + l.nights) return l;
      d += l.nights;
    }
    return legs.isEmpty ? null : legs.last;
  }

  /// 날짜 수가 바뀌면 일정 배열 길이를 맞춤 (줄어든 날의 카드는 마지막 날로)
  void fitDays() {
    final n = dayCount;
    while (days.length < n) {
      days.add([]);
    }
    while (days.length > n) {
      final extra = days.removeLast();
      days.last.addAll(extra);
    }
  }

  /// 오늘이 여행 중 몇째 날인지 (아니면 null)
  int? todayIndex([DateTime? now]) {
    final t = now ?? DateTime.now();
    final d = DateTime(t.year, t.month, t.day).difference(DateTime(start.year, start.month, start.day)).inDays;
    return (d >= 0 && d < dayCount) ? d : null;
  }

  double toKrw(double amount, String cur) => cur == 'KRW' ? amount : amount * (rates[cur] ?? 0);
  double get spent => expenses.fold(0.0, (a, e) => a + toKrw(e.amount, e.currency));

  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'start': start.toIso8601String(),
        'legs': legs.map((l) => l.toJson()).toList(),
        'days': days.map((d) => d.map((i) => i.toJson()).toList()).toList(),
        'wish': wish.map((i) => i.toJson()).toList(),
        'budget': budget,
        'rates': rates,
        'exp': expenses.map((e) => e.toJson()).toList(),
        'climate': climate.name,
        'climateManual': climateManual,
        'rainy': rainy,
        'acts': activities,
        'checks': checklist.map((c) => c.toJson()).toList(),
        'hidden': hiddenChecks,
      };

  factory Trip.fromJson(Map<String, dynamic> j) => Trip(
        id: j['id'],
        title: j['title'],
        start: DateTime.parse(j['start']),
        legs: (j['legs'] as List).map((e) => Leg.fromJson(e)).toList(),
        days: (j['days'] as List)
            .map((d) => (d as List).map((e) => TripItem.fromJson(e)).toList())
            .toList(),
        wish: (j['wish'] as List? ?? []).map((e) => TripItem.fromJson(e)).toList(),
        budget: j['budget'] ?? 0,
        rates: (j['rates'] as Map? ?? {}).map((k, v) => MapEntry('$k', (v as num).toDouble())),
        expenses: (j['exp'] as List? ?? []).map((e) => Expense.fromJson(e)).toList(),
        climate: Climate.values.firstWhere((c) => c.name == j['climate'], orElse: () => Climate.mild),
        climateManual: j['climateManual'] ?? false,
        rainy: j['rainy'] ?? false,
        activities: List<String>.from(j['acts'] ?? []),
        checklist: (j['checks'] as List? ?? []).map((e) => CheckItem.fromJson(e)).toList(),
        hiddenChecks: List<String>.from(j['hidden'] ?? []),
      );
}
