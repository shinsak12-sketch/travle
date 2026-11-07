const _wk = ['월', '화', '수', '목', '금', '토', '일'];
const _wkEn = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

String two(int n) => n.toString().padLeft(2, '0');
String md(DateTime d) => '${two(d.month)}.${two(d.day)}';
String mdWeek(DateTime d) => '${d.month}월 ${d.day}일 ${_wk[d.weekday - 1]}요일';
String wkKo(DateTime d) => _wk[d.weekday - 1];
String wkEn(DateTime d) => _wkEn[d.weekday - 1];
String ymd(DateTime d) => '${d.year}.${two(d.month)}.${two(d.day)}';

/// 1234567 → 1,234,567
String comma(num n, {int digits = 0}) {
  final neg = n < 0;
  final s = n.abs().toStringAsFixed(digits);
  final parts = s.split('.');
  final buf = StringBuffer();
  final int = parts[0];
  for (var i = 0; i < int.length; i++) {
    if (i > 0 && (int.length - i) % 3 == 0) buf.write(',');
    buf.write(int[i]);
  }
  final out = parts.length > 1 ? '$buf.${parts[1]}' : buf.toString();
  return neg ? '-$out' : out;
}

String won(num n) => '${comma(n.round())}원';

String money(double amount, String cur) {
  if (cur == 'KRW') return won(amount);
  final digits = amount == amount.roundToDouble() ? 0 : 2;
  return '${comma(amount, digits: digits)} $cur';
}

/// 'ICN 10:20' 같은 시간 문자열 정리
String hhmm(int h, int m) => '${two(h)}:${two(m)}';

int daysUntil(DateTime d, [DateTime? now]) {
  final n = now ?? DateTime.now();
  return DateTime(d.year, d.month, d.day).difference(DateTime(n.year, n.month, n.day)).inDays;
}
