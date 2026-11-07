// 보딩패스 디자인: 색·글꼴·공용 위젯
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class C {
  static const bg = Color(0xFFE9EDF2);
  static const paper = Color(0xFFFFFFFF);
  static const paper2 = Color(0xFFF4F6F9);
  static const ink = Color(0xFF14213D);
  static const ink2 = Color(0xFF2F3D5C);
  static const red = Color(0xFFE5383B);
  static const redSoft = Color(0xFFFFF1F1);
  static const muted = Color(0xFF5A6782);
  static const faint = Color(0xFF8A96AD);
  static const line = Color(0xFFD3D9E3);
  static const line2 = Color(0xFFC4CCD8);
  static const onInk = Color(0xFF9FB0D0);
  static const green = Color(0xFF1F8A5B);
}

const kSans = 'GothicA1';
const kMono = 'SpaceMono';

TextStyle sans(double size, {FontWeight w = FontWeight.w600, Color c = C.ink, double? h}) =>
    TextStyle(fontFamily: kSans, fontSize: size, fontWeight: w, color: c, height: h);

// Space Mono에 한글이 없어서 한글은 고딕으로 대신 그림
TextStyle mono(double size, {bool bold = false, Color c = C.ink, double ls = 0}) => TextStyle(
  fontFamily: kMono,
  fontFamilyFallback: const [kSans],
  fontSize: size,
  fontWeight: bold ? FontWeight.w700 : FontWeight.w400,
  color: c,
  letterSpacing: ls,
);

ThemeData buildTheme() {
  final base = ThemeData(
    useMaterial3: true,
    fontFamily: kSans,
    scaffoldBackgroundColor: C.bg,
    colorScheme: ColorScheme.fromSeed(seedColor: C.ink, primary: C.ink, secondary: C.red, surface: C.bg),
    splashFactory: NoSplash.splashFactory,
    highlightColor: Colors.transparent,
  );
  return base.copyWith(
    textTheme: base.textTheme.apply(fontFamily: kSans, bodyColor: C.ink, displayColor: C.ink),
    appBarTheme: const AppBarTheme(backgroundColor: C.bg, foregroundColor: C.ink, elevation: 0, scrolledUnderElevation: 0),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: C.paper,
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
      labelStyle: sans(14, c: C.muted),
      floatingLabelStyle: mono(12, c: C.ink, ls: 1),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: C.line2, width: 1.5),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: C.line2, width: 1.5),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: C.ink, width: 1.5),
      ),
    ),
    bottomSheetTheme: const BottomSheetThemeData(backgroundColor: C.bg, showDragHandle: true, dragHandleColor: C.line2),
    snackBarTheme: SnackBarThemeData(
      backgroundColor: C.ink,
      contentTextStyle: sans(14, c: Colors.white),
      behavior: SnackBarBehavior.floating,
    ),
    dialogTheme: DialogThemeData(
      backgroundColor: C.bg,
      titleTextStyle: sans(18, w: FontWeight.w800),
    ),
    datePickerTheme: const DatePickerThemeData(backgroundColor: C.bg),
    timePickerTheme: const TimePickerThemeData(backgroundColor: C.bg),
  );
}

/// 누르면 살짝 눌리며 기울어지는 동작
class Press extends StatefulWidget {
  final Widget child;
  final VoidCallback? onTap;
  final VoidCallback? onLongPress;
  final double tilt;
  const Press({super.key, required this.child, this.onTap, this.onLongPress, this.tilt = -0.006});
  @override
  State<Press> createState() => _PressState();
}

class _PressState extends State<Press> {
  bool _down = false;
  void _set(bool v) => setState(() => _down = v);
  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTapDown: widget.onTap == null ? null : (_) => _set(true),
      onTapUp: (_) => _set(false),
      onTapCancel: () => _set(false),
      onTap: widget.onTap == null
          ? null
          : () {
              HapticFeedback.selectionClick();
              widget.onTap!();
            },
      onLongPress: widget.onLongPress,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 160),
        curve: Curves.easeOut,
        transform: _down ? (Matrix4.translationValues(0, 2, 0)..rotateZ(widget.tilt)) : Matrix4.identity(),
        transformAlignment: Alignment.center,
        child: widget.child,
      ),
    );
  }
}

/// 목록이 나타날 때 차례로 미끄러져 들어오는 동작
class SlideIn extends StatelessWidget {
  final int index;
  final Widget child;
  final Offset from;
  const SlideIn({super.key, required this.index, required this.child, this.from = const Offset(-16, 0)});
  @override
  Widget build(BuildContext context) {
    final delay = (index.clamp(0, 8)) * 60;
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: Duration(milliseconds: 380 + delay),
      curve: Interval(delay / (380 + delay), 1, curve: Curves.easeOutBack),
      builder: (_, t, c) => Opacity(
        opacity: t.clamp(0, 1),
        child: Transform.translate(offset: from * (1 - t), child: c),
      ),
      child: child,
    );
  }
}

/// 버튼: 남색 테두리 + 아래로 딱딱한 그림자, 누르면 그림자로 내려앉음
class PassButton extends StatefulWidget {
  final String label;
  final VoidCallback? onTap;
  final bool primary;
  final IconData? icon;
  final double height;
  final bool dashed;
  const PassButton(this.label, {super.key, this.onTap, this.primary = false, this.icon, this.height = 54, this.dashed = false});
  @override
  State<PassButton> createState() => _PassButtonState();
}

class _PassButtonState extends State<PassButton> {
  bool _down = false;
  @override
  Widget build(BuildContext context) {
    final fg = widget.primary ? Colors.white : C.ink;
    final shadow = widget.dashed ? 0.0 : (_down ? 1.0 : 4.0);
    return GestureDetector(
      onTapDown: (_) => setState(() => _down = true),
      onTapUp: (_) => setState(() => _down = false),
      onTapCancel: () => setState(() => _down = false),
      onTap: widget.onTap == null
          ? null
          : () {
              HapticFeedback.lightImpact();
              widget.onTap!();
            },
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 110),
        height: widget.height,
        margin: EdgeInsets.only(top: 4 - shadow, bottom: shadow),
        decoration: BoxDecoration(
          color: widget.dashed ? Colors.transparent : (widget.primary ? C.red : C.paper),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: C.ink, width: 1.5),
          boxShadow: shadow > 0 ? [BoxShadow(color: C.ink, offset: Offset(0, shadow))] : null,
        ),
        alignment: Alignment.center,
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (widget.icon != null) ...[Icon(widget.icon, size: 18, color: fg), const SizedBox(width: 6)],
            Flexible(
              child: Text(
                widget.label,
                style: sans(15, w: FontWeight.w800, c: fg),
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// 절취선 (가로/세로)
class Dashed extends StatelessWidget {
  final bool vertical;
  final Color color;
  const Dashed({super.key, this.vertical = false, this.color = C.line});
  @override
  Widget build(BuildContext context) => CustomPaint(
    size: vertical ? const Size(2, double.infinity) : const Size(double.infinity, 2),
    painter: _DashPainter(vertical, color),
  );
}

class _DashPainter extends CustomPainter {
  final bool v;
  final Color color;
  _DashPainter(this.v, this.color);
  @override
  void paint(Canvas canvas, Size s) {
    final p = Paint()
      ..color = color
      ..strokeWidth = 2;
    final len = v ? s.height : s.width;
    for (double x = 0; x < len; x += 9) {
      final e = (x + 5).clamp(0, len).toDouble();
      canvas.drawLine(v ? Offset(1, x) : Offset(x, 1), v ? Offset(1, e) : Offset(e, 1), p);
    }
  }

  @override
  bool shouldRepaint(_DashPainter o) => o.v != v || o.color != color;
}

/// 티켓: 왼쪽 스텁 + 세로 절취선 + 반원 홈
class Ticket extends StatelessWidget {
  final Widget? stub;
  final double stubWidth;
  final Widget body;
  final Widget? trailing;
  final Color color;
  final Color notch;
  final bool shadow;
  final BoxBorder? border;
  const Ticket({
    super.key,
    this.stub,
    this.stubWidth = 74,
    required this.body,
    this.trailing,
    this.color = C.paper,
    this.notch = C.bg,
    this.shadow = true,
    this.border,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(14),
        border: border,
        boxShadow: shadow ? const [BoxShadow(color: Color(0x1F14213D), blurRadius: 14, offset: Offset(0, 6))] : null,
      ),
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (stub != null) ...[
                  SizedBox(
                    width: stubWidth,
                    child: Center(child: stub),
                  ),
                  const Padding(padding: EdgeInsets.symmetric(vertical: 6), child: Dashed(vertical: true)),
                ],
                Expanded(
                  child: Padding(padding: const EdgeInsets.fromLTRB(14, 12, 10, 12), child: body),
                ),
                ?trailing,
              ],
            ),
          ),
          if (stub != null) ...[
            Positioned(left: stubWidth - 6, top: -7, child: _notch()),
            Positioned(left: stubWidth - 6, bottom: -7, child: _notch()),
          ],
        ],
      ),
    );
  }

  Widget _notch() => Container(
    width: 14,
    height: 14,
    decoration: BoxDecoration(color: notch, shape: BoxShape.circle),
  );
}

/// 작은 라벨 (모노, 자간 넓게)
class Eyebrow extends StatelessWidget {
  final String text;
  final Color color;
  const Eyebrow(this.text, {super.key, this.color = C.faint});
  @override
  Widget build(BuildContext context) => Text(text, style: mono(11, c: color, ls: 1.6));
}

/// 칩: 선택되면 남색으로 채워짐
class PassChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback? onTap;
  final Color selectedColor;
  const PassChip(this.label, {super.key, this.selected = false, this.onTap, this.selectedColor = C.ink});
  @override
  Widget build(BuildContext context) {
    return Press(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 160),
        constraints: const BoxConstraints(minHeight: 38),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: selected ? selectedColor : Colors.transparent,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: selected ? selectedColor : C.line2, width: 1.5),
        ),
        child: Text(
          label,
          style: sans(13, w: selected ? FontWeight.w800 : FontWeight.w600, c: selected ? Colors.white : C.muted),
        ),
      ),
    );
  }
}

/// 도장 (USED 등)
class Stamp extends StatelessWidget {
  final String text;
  final double size;
  const Stamp(this.text, {super.key, this.size = 11});
  @override
  Widget build(BuildContext context) => Transform.rotate(
    angle: -0.12,
    child: Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
      decoration: BoxDecoration(
        border: Border.all(color: C.red, width: 1.5),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(text, style: mono(size, bold: true, c: C.red, ls: 1.2)),
    ),
  );
}

/// 빈 화면 안내
class EmptyNote extends StatelessWidget {
  final String title;
  final String body;
  const EmptyNote(this.title, this.body, {super.key});
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.all(18),
    decoration: BoxDecoration(
      borderRadius: BorderRadius.circular(14),
      border: Border.all(color: C.line2, width: 1.5),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: sans(15, w: FontWeight.w800)),
        const SizedBox(height: 4),
        Text(body, style: sans(13, c: C.muted, h: 1.5)),
      ],
    ),
  );
}
