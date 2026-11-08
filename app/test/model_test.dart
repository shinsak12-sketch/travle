import 'package:flutter_test/flutter_test.dart';
import 'package:travle/data/checklist_templates.dart';
import 'package:travle/data/models.dart';
import 'package:travle/data/store.dart';
import 'package:flutter/services.dart';
import 'package:travle/ui/sheet.dart';

Trip sample() => Trip(
  title: '교토·오사카',
  start: DateTime(2026, 11, 20),
  legs: [
    Leg(destId: 'JP-OSA', cityKo: '오사카', code: 'OSA', nights: 2),
    Leg(destId: 'JP-KYOTO', cityKo: '교토', code: 'KYO', nights: 2),
  ],
);

void main() {
  test('일자 수와 도시 배정', () {
    final t = sample();
    expect(t.dayCount, 5);
    expect(t.days.length, 5);
    expect(t.legOf(0)!.cityKo, '오사카');
    expect(t.legOf(1)!.cityKo, '오사카');
    expect(t.legOf(2)!.cityKo, '교토');
    expect(t.legOf(4)!.cityKo, '교토');
    expect(t.end, DateTime(2026, 11, 24));
  });

  test('구간이 줄면 남는 날의 카드는 마지막 날로', () {
    final t = sample();
    t.days[4].add(TripItem(type: ItemType.memo, title: '마지막 날'));
    t.legs.last.nights = 1;
    t.fitDays();
    expect(t.days.length, 4);
    expect(t.days[3].single.title, '마지막 날');
  });

  test('JSON 저장 후 그대로 복원', () {
    final t = sample();
    t.days[0].add(TripItem(type: ItemType.flight, title: 'KE723', time: '08:10', f: {'from': 'ICN', 'to': 'KIX'}, cost: 320000));
    t.wish.add(TripItem(type: ItemType.place, title: '기요미즈데라', destId: 'JP-KYOTO', cat: 1, lat: 34.99, lon: 135.78));
    t.expenses.add(Expense(title: '라멘', category: '식비', amount: 1200, currency: 'JPY', date: DateTime(2026, 11, 21)));
    t.rates['JPY'] = 9.2;
    t.activities.add('golf');
    syncChecklist(t);
    t.checklist.first.done = true;

    final back = Trip.fromJson(t.toJson());
    expect(back.toJson(), t.toJson());
    expect(back.days[0].first.get('to'), 'KIX');
    expect(back.checklist.first.done, true);
    expect(back.spent, closeTo(1200 * 9.2, .01));
  });

  test('체크리스트: 날씨·활동을 바꾸면 항목이 바뀌고, 체크와 직접 추가는 유지', () {
    final t = sample()..climate = Climate.cold;
    syncChecklist(t);
    expect(t.checklist.any((c) => c.text == '핫팩'), true);
    expect(t.checklist.any((c) => c.text == '선크림'), false);

    final passport = t.checklist.firstWhere((c) => c.text.startsWith('여권 ('));
    passport.done = true;
    t.checklist.add(CheckItem(text: '국제운전면허증', group: '서류', src: 'custom'));

    t.climate = Climate.hot;
    t.activities.add('freediving');
    syncChecklist(t);
    expect(t.checklist.any((c) => c.text == '핫팩'), false);
    expect(t.checklist.any((c) => c.text == '선크림'), true);
    expect(t.checklist.any((c) => c.text == '롱핀'), true);
    expect(t.checklist.firstWhere((c) => c.text.startsWith('여권 (')).done, true);
    expect(t.checklist.any((c) => c.text == '국제운전면허증'), true);

    // 지운 템플릿 항목은 다시 안 생김
    final fan = t.checklist.firstWhere((c) => c.text == '휴대용 선풍기');
    hideCheck(t, fan);
    syncChecklist(t);
    expect(t.checklist.any((c) => c.text == '휴대용 선풍기'), false);

    t.activities.remove('freediving');
    syncChecklist(t);
    expect(t.checklist.any((c) => c.text == '롱핀'), false);
  });

  test('날씨 추정: 위도와 출발 월', () {
    expect(guessClimate(10.3, DateTime(2026, 1, 5)), Climate.hot); // 세부
    expect(guessClimate(35.0, DateTime(2026, 1, 5)), Climate.cold); // 교토 겨울
    expect(guessClimate(35.0, DateTime(2026, 7, 5)), Climate.hot); // 교토 여름
    expect(guessClimate(35.0, DateTime(2026, 11, 5)), Climate.mild);
    expect(guessClimate(-45.0, DateTime(2026, 7, 5)), Climate.cold); // 퀸스타운 겨울
    expect(guessClimate(-45.0, DateTime(2026, 12, 25)), Climate.hot);
  });

  test('카드 금액은 지출로 연결되고, 카드를 지우면 지출도 지워짐', () {
    final s = Store();
    final t = sample();
    s.trips.add(t);
    final stay = TripItem(type: ItemType.stay, title: '교토역 호텔', cost: 30000, currency: 'JPY');
    s.addItem(t, stay, day: 2);
    expect(t.expenses.single.category, '숙소');
    expect(t.expenses.single.date, DateTime(2026, 11, 22));

    stay.cost = 32000;
    s.updateItem(t, stay);
    expect(t.expenses.single.amount, 32000);

    s.moveItem(t, stay, 3);
    expect(t.expenses.single.date, DateTime(2026, 11, 23));

    s.removeItem(t, stay);
    expect(t.expenses, isEmpty);
  });

  test('시간 있는 카드는 시간 순으로 정렬', () {
    final s = Store();
    final t = sample();
    s.trips.add(t);
    s.addItem(t, TripItem(type: ItemType.place, title: 'B', time: '13:00'), day: 0);
    s.addItem(t, TripItem(type: ItemType.place, title: 'A', time: '09:00'), day: 0);
    expect(t.days[0].map((i) => i.title), ['A', 'B']);
    s.reorder(t, 0, 0, 1);
    expect(t.days[0].map((i) => i.title), ['B', 'A']);
  });

  test('금액 입력 쉼표', () {
    const f = ThousandsFormatter();
    TextEditingValue type(String t) => f.formatEditUpdate(
      TextEditingValue.empty,
      TextEditingValue(
        text: t,
        selection: TextSelection.collapsed(offset: t.length),
      ),
    );
    expect(type('500000').text, '500,000');
    expect(type('1234567.891').text, '1,234,567.89');
    expect(type('9.2').text, '9.2');
    expect(type('0012').text, '12');
    expect(type('12,34a5').text, '12,345');
    expect(
      const ThousandsFormatter(decimal: false).formatEditUpdate(TextEditingValue.empty, const TextEditingValue(text: '1.5')).text,
      '15',
    );
    expect(parseAmount('1,234,567.5'), 1234567.5);
    expect(amountText(2400), '2,400');
  });

  test('손상된 백업은 불러오지 않고 지금 데이터 유지', () {
    final s = Store();
    s.trips.add(sample());
    expect(() => s.importJson('{"trips":[{"title":"x"}]}'), throwsA(anything));
    expect(() => s.importJson('not json'), throwsA(anything));
    expect(s.trips.single.title, '교토·오사카');
    final good = s.exportJson();
    s.importJson(good);
    expect(s.trips.single.title, '교토·오사카');
  });
}
