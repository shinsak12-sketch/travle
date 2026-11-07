// 담은 곳 → 날짜에 넣기 (아래에서 올라오는 시트)
import 'package:flutter/material.dart';

import '../data/models.dart';
import '../data/store.dart';
import '../ui/item_card.dart';
import '../ui/route.dart';
import '../ui/theme.dart';
import '../util/format.dart';
import 'guide_screen.dart';
import 'item_edit_screen.dart';

Future<void> showWishSheet(BuildContext context, String tripId, {int? defaultDay}) => showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (_) => _WishSheet(tripId: tripId, defaultDay: defaultDay),
    );

class _WishSheet extends StatefulWidget {
  final String tripId;
  final int? defaultDay;
  const _WishSheet({required this.tripId, this.defaultDay});
  @override
  State<_WishSheet> createState() => _WishSheetState();
}

class _WishSheetState extends State<_WishSheet> {
  String? _open;

  @override
  Widget build(BuildContext context) {
    final store = StoreScope.of(context);
    final t = store.byId(widget.tripId);
    if (t == null) return const SizedBox();
    final cats = store.catalog!.cats;
    final h = MediaQuery.of(context).size.height * .78;

    return SizedBox(
      height: h,
      child: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(18, 0, 18, 10),
          child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
            Text('담은 곳', style: sans(20, w: FontWeight.w800)),
            const SizedBox(width: 8),
            Expanded(child: Text('${t.wish.length} · 일정에 아직 안 넣음', style: mono(12, c: C.muted))),
          ]),
        ),
        Expanded(
          child: t.wish.isEmpty
              ? ListView(padding: const EdgeInsets.symmetric(horizontal: 18), children: const [
                  EmptyNote('담은 곳이 없음', '"장소 찾기"에서 담기를 누르거나, 아래에서 직접 추가하면 여기에 모임. 나중에 날짜를 골라 일정에 넣음.'),
                ])
              : ListView.separated(
                  padding: const EdgeInsets.fromLTRB(18, 0, 18, 12),
                  itemCount: t.wish.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 8),
                  itemBuilder: (_, i) => SlideIn(index: i, from: const Offset(0, 12), child: _row(store, t, t.wish[i], cats)),
                ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(18, 4, 18, 14),
          child: Row(children: [
            Expanded(
              child: PassButton('장소 찾기', icon: Icons.travel_explore_rounded, onTap: () {
                final leg = t.legOf(widget.defaultDay ?? 0);
                Navigator.pop(context);
                Navigator.push(context, slideRoute(GuideScreen(tripId: t.id, destId: leg?.destId ?? '', day: null)));
              }),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: PassButton('+ 직접 추가', dashed: true, onTap: () {
                Navigator.pop(context);
                Navigator.push(context, slideRoute(ItemEditScreen(tripId: t.id, newType: ItemType.food, day: null)));
              }),
            ),
          ]),
        ),
      ]),
    );
  }

  Widget _row(Store store, Trip t, TripItem it, List<String> cats) {
    final open = _open == it.id;
    final city = it.destId == null ? null : store.catalog!.byId[it.destId!];
    final label = [
      if (city != null) city.code,
      if (it.type == ItemType.place && it.cat != null && it.cat! < cats.length) cats[it.cat!] else it.type.label,
    ].join(' · ');
    return AnimatedContainer(
      duration: const Duration(milliseconds: 200),
      padding: const EdgeInsets.fromLTRB(14, 10, 6, 10),
      decoration: BoxDecoration(
        color: C.paper,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: open ? C.ink : Colors.transparent, width: 1.5),
        boxShadow: [
          open ? const BoxShadow(color: C.ink, offset: Offset(0, 4)) : const BoxShadow(color: Color(0x1414213D), blurRadius: 10, offset: Offset(0, 4)),
        ],
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Icon(itemIcon(it.type), size: 18, color: C.muted),
          const SizedBox(width: 10),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(label, style: mono(10, c: C.red, ls: 1)),
              Text(it.title, style: sans(15, w: FontWeight.w800)),
            ]),
          ),
          if (open)
            IconButton(
              tooltip: '담은 곳에서 빼기',
              onPressed: () => store.removeItem(t, it),
              icon: const Icon(Icons.delete_outline_rounded, color: C.faint, size: 20),
            )
          else
            TextButton(
              onPressed: () => setState(() => _open = it.id),
              child: Text('날짜 선택', style: sans(13, w: FontWeight.w800)),
            ),
        ]),
        if (open) ...[
          const SizedBox(height: 8),
          Text('어느 날에 넣을까?', style: mono(10, c: C.faint, ls: 1)),
          const SizedBox(height: 6),
          Wrap(spacing: 6, runSpacing: 6, children: [
            for (var d = 0; d < t.dayCount; d++) _dayChip(store, t, it, d),
          ]),
        ],
      ]),
    );
  }

  Widget _dayChip(Store store, Trip t, TripItem it, int d) {
    final date = t.dateOf(d);
    final city = t.legOf(d)?.cityKo ?? '';
    return Press(
      onTap: () {
        store.moveItem(t, it, d);
        setState(() => _open = null);
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text('${it.title} → D${d + 1} ${md(date)}')));
      },
      child: Container(
        width: 62,
        padding: const EdgeInsets.symmetric(vertical: 6),
        decoration: BoxDecoration(
          color: d == widget.defaultDay ? C.redSoft : C.paper,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: d == widget.defaultDay ? C.red : C.line2, width: 1.5),
        ),
        child: Column(children: [
          Text('D${d + 1}', style: mono(13, bold: true)),
          Text(city, style: sans(10, c: C.muted), overflow: TextOverflow.ellipsis),
          Text('${t.days[d].length}곳', style: mono(9.5, c: C.faint)),
        ]),
      ),
    );
  }
}
