// 일정 카드: 장소 · 항공편 · 숙소 · 쇼핑 · 맛집 · 메모
import 'package:flutter/material.dart';

import '../data/models.dart';
import '../util/format.dart';
import 'theme.dart';

IconData itemIcon(ItemType t) => const {
  ItemType.place: Icons.place_outlined,
  ItemType.flight: Icons.flight_rounded,
  ItemType.stay: Icons.hotel_rounded,
  ItemType.shopping: Icons.shopping_bag_outlined,
  ItemType.food: Icons.restaurant_rounded,
  ItemType.memo: Icons.sticky_note_2_outlined,
}[t]!;

class ItemCard extends StatelessWidget {
  final TripItem item;
  final int number;
  final List<String> cats;
  final VoidCallback? onTap;
  final VoidCallback? onMaps;
  const ItemCard({super.key, required this.item, required this.number, required this.cats, this.onTap, this.onMaps});

  @override
  Widget build(BuildContext context) {
    final card = switch (item.type) {
      ItemType.flight => _flight(),
      ItemType.stay => _stay(),
      ItemType.memo => _memo(),
      _ => _ticket(),
    };
    return Press(
      onTap: onTap,
      child: AnimatedOpacity(duration: const Duration(milliseconds: 200), opacity: item.done ? .55 : 1, child: card),
    );
  }

  String get _label {
    final n = two(number);
    return switch (item.type) {
      ItemType.place => '$n · ${item.cat != null && item.cat! < cats.length ? cats[item.cat!] : '장소'}',
      ItemType.shopping => '$n · 쇼핑',
      ItemType.food => '$n · 맛집',
      _ => '$n · ${item.type.label}',
    };
  }

  Widget? _mapsButton({Color c = C.ink, Color border = const Color(0xFFEDF0F4)}) => onMaps == null
      ? null
      : Container(
          width: 50,
          decoration: BoxDecoration(
            border: Border(left: BorderSide(color: border)),
          ),
          child: IconButton(
            tooltip: '구글지도로 열기',
            onPressed: onMaps,
            icon: Icon(Icons.near_me_outlined, color: c, size: 20),
          ),
        );

  List<Widget> _extraLines() {
    final lines = <String>[
      if (item.type == ItemType.food && item.get('menu').isNotEmpty) '메뉴: ${item.get('menu')}',
      if (item.type == ItemType.food && item.get('reservation').isNotEmpty) '예약: ${item.get('reservation')}',
      if (item.type == ItemType.shopping && item.get('buy').isNotEmpty) '살 것: ${item.get('buy')}',
      if (item.memo.isNotEmpty) item.memo,
      if (item.cost != null && item.cost! > 0) money(item.cost!, item.currency),
    ];
    return [
      for (final l in lines)
        Padding(
          padding: const EdgeInsets.only(top: 3),
          child: Text(
            l,
            style: sans(12.5, c: C.muted, h: 1.4),
            maxLines: 3,
            overflow: TextOverflow.ellipsis,
          ),
        ),
    ];
  }

  Widget _ticket() {
    return Ticket(
      stub: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (item.time.isEmpty)
            Icon(itemIcon(item.type), size: 20, color: C.ink)
          else ...[
            Text('TIME', style: mono(10, c: C.faint)),
            Text(item.time, style: mono(17, bold: true)),
          ],
          if (item.done) const Padding(padding: EdgeInsets.only(top: 4), child: Stamp('USED', size: 9)),
        ],
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(_label, style: mono(10, c: C.red, ls: 1)),
          const SizedBox(height: 2),
          Text(item.title, style: sans(16, w: FontWeight.w800, h: 1.25)),
          ..._extraLines(),
        ],
      ),
      trailing: _mapsButton(),
    );
  }

  Widget _flight() {
    final from = item.get('from'), to = item.get('to');
    final dep = item.time, arr = item.get('arrive');
    return Container(
      decoration: BoxDecoration(
        color: C.ink,
        borderRadius: BorderRadius.circular(14),
        boxShadow: const [BoxShadow(color: Color(0x3314213D), blurRadius: 14, offset: Offset(0, 6))],
      ),
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text('${two(number)} · FLIGHT', style: mono(10, c: C.onInk, ls: 1)),
              const Spacer(),
              Text(item.title, style: mono(13, bold: true, c: Colors.white)),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              _ap(from.isEmpty ? '---' : from, dep, CrossAxisAlignment.start),
              const Expanded(
                child: Padding(
                  padding: EdgeInsets.only(bottom: 14, left: 8, right: 8),
                  child: Row(
                    children: [
                      Expanded(child: Dashed(color: C.onInk)),
                      Icon(Icons.flight_rounded, size: 16, color: C.onInk),
                    ],
                  ),
                ),
              ),
              _ap(to.isEmpty ? '---' : to, arr, CrossAxisAlignment.end),
            ],
          ),
          if (item.get('seat').isNotEmpty || item.get('booking').isNotEmpty || item.get('terminal').isNotEmpty) ...[
            const SizedBox(height: 8),
            const Dashed(color: C.ink2),
            const SizedBox(height: 8),
            Wrap(
              spacing: 16,
              runSpacing: 4,
              children: [
                if (item.get('terminal').isNotEmpty) _kv('TERMINAL', item.get('terminal')),
                if (item.get('seat').isNotEmpty) _kv('SEAT', item.get('seat')),
                if (item.get('booking').isNotEmpty) _kv('BOOKING', item.get('booking')),
              ],
            ),
          ],
          if (item.memo.isNotEmpty || item.cost != null) ...[
            const SizedBox(height: 6),
            Text(
              [if (item.memo.isNotEmpty) item.memo, if (item.cost != null) money(item.cost!, item.currency)].join(' · '),
              style: sans(12.5, c: C.onInk),
            ),
          ],
        ],
      ),
    );
  }

  Widget _ap(String code, String time, CrossAxisAlignment al) => Column(
    crossAxisAlignment: al,
    children: [
      Text(code, style: mono(26, bold: true, c: Colors.white).copyWith(height: 1.1)),
      Text(time.isEmpty ? '--:--' : time, style: mono(12, c: C.onInk)),
    ],
  );

  Widget _kv(String k, String v) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(k, style: mono(9, c: C.onInk)),
      Text(v, style: mono(13, bold: true, c: Colors.white)),
    ],
  );

  Widget _stay() {
    final inTime = item.time, out = item.get('checkout');
    return Ticket(
      color: const Color(0xFFF2F5FB),
      border: Border.all(color: C.ink, width: 1.5),
      stub: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.hotel_rounded, color: C.ink, size: 22),
          const SizedBox(height: 2),
          Text(item.get('nights').isEmpty ? 'STAY' : '${item.get('nights')}N', style: mono(11, bold: true)),
        ],
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('${two(number)} · 숙소', style: mono(10, c: C.red, ls: 1)),
          const SizedBox(height: 2),
          Text(item.title, style: sans(16, w: FontWeight.w800, h: 1.25)),
          const SizedBox(height: 4),
          Wrap(
            spacing: 14,
            children: [
              if (inTime.isNotEmpty) Text('IN $inTime', style: mono(11.5, bold: true, c: C.muted)),
              if (out.isNotEmpty) Text('OUT $out', style: mono(11.5, bold: true, c: C.muted)),
              if (item.get('booking').isNotEmpty) Text('#${item.get('booking')}', style: mono(11.5, c: C.muted)),
            ],
          ),
          if (item.get('address').isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 3),
              child: Text(
                item.get('address'),
                style: sans(12, c: C.muted),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          if (item.memo.isNotEmpty || item.cost != null)
            Padding(
              padding: const EdgeInsets.only(top: 3),
              child: Text(
                [if (item.memo.isNotEmpty) item.memo, if (item.cost != null) money(item.cost!, item.currency)].join(' · '),
                style: sans(12.5, c: C.muted),
              ),
            ),
        ],
      ),
      trailing: _mapsButton(border: C.line),
    );
  }

  Widget _memo() => Container(
    padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
    decoration: BoxDecoration(
      color: const Color(0xFFFFF8D6),
      borderRadius: BorderRadius.circular(12),
      border: Border.all(color: const Color(0xFFE8D98A), width: 1.5),
    ),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Icon(Icons.sticky_note_2_outlined, size: 18, color: C.muted),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text([if (item.time.isNotEmpty) item.time, item.title].join(' · '), style: sans(14.5, w: FontWeight.w800)),
              if (item.memo.isNotEmpty) Text(item.memo, style: sans(13, c: C.muted, h: 1.45)),
            ],
          ),
        ),
      ],
    ),
  );
}
