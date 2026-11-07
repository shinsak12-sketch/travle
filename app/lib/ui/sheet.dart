// 아래에서 올라오는 시트: 폰 하단 버튼(내비게이션 바)과 키보드를 피해서 그림
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../util/format.dart';

Future<T?> showPassSheet<T>(BuildContext context, WidgetBuilder builder) {
  HapticFeedback.selectionClick();
  return showModalBottomSheet<T>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (c) => Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(c).viewInsets.bottom),
      child: SafeArea(top: false, child: builder(c)),
    ),
  );
}

/// 금액 입력: 칠 때마다 1,234,567 처럼 쉼표 (소수점 허용)
class ThousandsFormatter extends TextInputFormatter {
  final bool decimal;
  const ThousandsFormatter({this.decimal = true});

  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
    var raw = newValue.text.replaceAll(RegExp(decimal ? r'[^0-9.]' : r'[^0-9]'), '');
    if (raw.isEmpty) return const TextEditingValue();
    final dot = raw.indexOf('.');
    String intPart = dot < 0 ? raw : raw.substring(0, dot);
    String frac = dot < 0 ? '' : raw.substring(dot + 1).replaceAll('.', '');
    if (frac.length > 2) frac = frac.substring(0, 2);
    intPart = intPart.replaceFirst(RegExp(r'^0+(?=\d)'), '');
    if (intPart.isEmpty) intPart = '0';
    final text = comma(int.parse(intPart)) + (dot < 0 ? '' : '.$frac');
    // 커서는 끝에서부터 같은 거리 유지
    final fromEnd = newValue.text.length - newValue.selection.extentOffset;
    final pos = (text.length - fromEnd).clamp(0, text.length);
    return TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: pos),
    );
  }
}

/// 입력하던 걸 버리고 나갈지 묻기
Future<bool> confirmDiscard(BuildContext context) async {
  final r = await showDialog<bool>(
    context: context,
    builder: (c) => AlertDialog(
      title: const Text('입력한 내용 버리기'),
      content: const Text('저장하지 않고 나가면 지금 입력한 내용이 사라짐.'),
      actions: [
        TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('계속 입력')),
        TextButton(
          onPressed: () => Navigator.pop(c, true),
          child: const Text(
            '버리기',
            style: TextStyle(color: Color(0xFFE5383B), fontWeight: FontWeight.w800),
          ),
        ),
      ],
    ),
  );
  return r == true;
}

double? parseAmount(String s) => double.tryParse(s.replaceAll(',', '').trim());

String amountText(double? v) {
  if (v == null) return '';
  if (v == v.roundToDouble()) return comma(v.round());
  return comma(v, digits: 2);
}
