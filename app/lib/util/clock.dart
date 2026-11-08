// 현지 시각과 한국과의 시차 (IANA 시간대, 서머타임 반영)
import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:timezone/timezone.dart' as tz;

import 'format.dart';

tz.Location? _loc(String name) {
  if (name.isEmpty) return null;
  try {
    return tz.getLocation(name);
  } catch (_) {
    return null;
  }
}

/// 현지 시각 (시간대를 모르면 null)
DateTime? localNow(String tzName, [DateTime? now]) {
  final l = _loc(tzName);
  if (l == null) return null;
  return tz.TZDateTime.from(now ?? DateTime.now(), l);
}

/// 한국 기준 시차 문구: "한국보다 1시간 느림", "한국과 같음", "한국보다 3시간 30분 빠름"
String? gapFromKorea(String tzName, [DateTime? now]) {
  final l = _loc(tzName);
  final seoul = _loc('Asia/Seoul');
  if (l == null || seoul == null) return null;
  final at = now ?? DateTime.now();
  final diff = tz.TZDateTime.from(at, l).timeZoneOffset - tz.TZDateTime.from(at, seoul).timeZoneOffset;
  if (diff == Duration.zero) return '한국과 같음';
  final m = diff.inMinutes.abs();
  final text = '${m ~/ 60}시간${m % 60 == 0 ? '' : ' ${m % 60}분'}';
  return '한국보다 $text ${diff.isNegative ? '느림' : '빠름'}';
}

/// "현지 14:05 · 한국보다 1시간 느림" (30초마다 갱신)
class LocalClock extends StatefulWidget {
  final String tzName;
  final TextStyle style;
  final String prefix;
  const LocalClock({super.key, required this.tzName, required this.style, this.prefix = '현지'});
  @override
  State<LocalClock> createState() => _LocalClockState();
}

class _LocalClockState extends State<LocalClock> {
  Timer? _t;
  @override
  void initState() {
    super.initState();
    _t = Timer.periodic(const Duration(seconds: 30), (_) => setState(() {}));
  }

  @override
  void dispose() {
    _t?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final n = localNow(widget.tzName);
    final gap = gapFromKorea(widget.tzName);
    if (n == null || gap == null) return const SizedBox.shrink();
    return Text('${widget.prefix} ${hhmm(n.hour, n.minute)} · $gap', style: widget.style);
  }
}
