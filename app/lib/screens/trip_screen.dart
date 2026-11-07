// 여행 화면: 일정 · 체크리스트 · 예산
import 'package:flutter/material.dart';

import '../data/models.dart';
import '../data/store.dart';
import '../ui/item_card.dart';
import '../ui/route.dart';
import '../ui/theme.dart';
import '../util/format.dart';
import '../util/maps.dart';
import 'budget_tab.dart';
import 'checklist_tab.dart';
import 'guide_screen.dart';
import 'item_edit_screen.dart';
import 'today_screen.dart';
import 'trip_edit_screen.dart';
import 'wish_sheet.dart';

class TripScreen extends StatefulWidget {
  final String tripId;
  final int initialTab;
  const TripScreen({super.key, required this.tripId, this.initialTab = 0});
  @override
  State<TripScreen> createState() => _TripScreenState();
}

class _TripScreenState extends State<TripScreen> {
  late int _tab = widget.initialTab;
  int _day = 0;
  bool _dayInit = false;

  @override
  Widget build(BuildContext context) {
    final store = StoreScope.of(context);
    final t = store.byId(widget.tripId);
    if (t == null) return const Scaffold();
    if (!_dayInit) {
      _dayInit = true;
      _day = t.todayIndex() ?? 0;
    }
    _day = _day.clamp(0, t.dayCount - 1);

    return Scaffold(
      body: SafeArea(
        child: Column(children: [
          _header(context, t),
          Padding(
            padding: const EdgeInsets.fromLTRB(18, 10, 18, 4),
            child: _Tabs(
              labels: const ['일정', '체크리스트', '예산'],
              index: _tab,
              onTap: (i) => setState(() => _tab = i),
            ),
          ),
          Expanded(
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 220),
              child: switch (_tab) {
                0 => _schedule(context, store, t),
                1 => ChecklistTab(key: const ValueKey('c'), trip: t),
                _ => BudgetTab(key: const ValueKey('b'), trip: t),
              },
            ),
          ),
        ]),
      ),
    );
  }

  Widget _header(BuildContext context, Trip t) {
    final first = t.legs.firstOrNull?.code ?? '---';
    final last = t.legs.length > 1 ? t.legs.last.code : null;
    final live = t.todayIndex() != null;
    return Container(
      margin: const EdgeInsets.fromLTRB(18, 10, 18, 0),
      padding: const EdgeInsets.fromLTRB(4, 6, 14, 12),
      decoration: BoxDecoration(
        color: C.ink,
        borderRadius: BorderRadius.circular(20),
        boxShadow: const [BoxShadow(color: Color(0x4D14213D), blurRadius: 22, offset: Offset(0, 10))],
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          IconButton(
            tooltip: '뒤로',
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back_ios_new_rounded, color: Colors.white, size: 18),
          ),
          Expanded(child: Eyebrow('TRIP PASS · ${t.nights}N${t.dayCount}D', color: C.onInk)),
          if (live)
            _pill('오늘', () => Navigator.pushReplacement(context, slideRoute(TodayScreen(tripId: t.id))))
          else
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(color: C.red, borderRadius: BorderRadius.circular(6)),
              child: Text(daysUntil(t.start) >= 0 ? 'D-${daysUntil(t.start)}' : 'DONE', style: mono(12, bold: true, c: Colors.white)),
            ),
          IconButton(
            tooltip: '여행 정보 고치기',
            onPressed: () => Navigator.push(context, slideRoute(TripEditScreen(tripId: t.id))),
            icon: const Icon(Icons.edit_outlined, color: Colors.white, size: 18),
          ),
        ]),
        Padding(
          padding: const EdgeInsets.only(left: 14),
          child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
            Text(first, style: mono(28, bold: true, c: Colors.white).copyWith(height: 1)),
            if (last != null) ...[
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 8),
                child: Icon(Icons.arrow_forward_rounded, color: C.onInk, size: 18),
              ),
              Text(last, style: mono(28, bold: true, c: Colors.white).copyWith(height: 1)),
            ],
            const SizedBox(width: 12),
            Expanded(
              child: Text(t.title,
                  style: sans(15, w: FontWeight.w800, c: Colors.white), overflow: TextOverflow.ellipsis),
            ),
          ]),
        ),
        Padding(
          padding: const EdgeInsets.only(left: 14, top: 4),
          child: Text('${md(t.start)} ${wkEn(t.start)} – ${md(t.end)} ${wkEn(t.end)}', style: mono(11.5, c: C.onInk)),
        ),
      ]),
    );
  }

  Widget _pill(String label, VoidCallback onTap) => Press(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
          decoration: BoxDecoration(color: C.red, borderRadius: BorderRadius.circular(8)),
          child: Text(label, style: sans(12.5, w: FontWeight.w800, c: Colors.white)),
        ),
      );

  // ---------------- 일정 ----------------
  Widget _schedule(BuildContext context, Store store, Trip t) {
    final items = t.days[_day];
    final leg = t.legOf(_day);
    final cat = store.catalog!;
    final cityEn = leg == null ? '' : (cat.byId[leg.destId]?.en ?? '');
    return Column(key: const ValueKey('s'), children: [
      SizedBox(
        height: 64,
        child: ListView.separated(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.fromLTRB(18, 8, 18, 4),
          itemCount: t.dayCount,
          separatorBuilder: (_, _) => const SizedBox(width: 6),
          itemBuilder: (_, i) {
            final sel = i == _day;
            final date = t.dateOf(i);
            return Press(
              onTap: () => setState(() => _day = i),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 180),
                width: 58,
                margin: EdgeInsets.only(bottom: sel ? 0 : 4, top: sel ? 0 : 0),
                decoration: BoxDecoration(
                  color: sel ? C.paper : Colors.transparent,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: sel ? C.ink : C.line2, width: 1.5),
                  boxShadow: sel ? const [BoxShadow(color: C.ink, offset: Offset(0, 4))] : null,
                ),
                child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                  Text('D${i + 1}', style: mono(14, bold: true, c: sel ? C.ink : C.muted)),
                  Text('${two(date.day)} ${wkEn(date)}', style: mono(9.5, c: sel ? C.ink : C.faint)),
                ]),
              ),
            );
          },
        ),
      ),
      Padding(
        padding: const EdgeInsets.fromLTRB(18, 10, 18, 8),
        child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
          Text(leg?.cityKo ?? '', style: sans(22, w: FontWeight.w800)),
          const SizedBox(width: 8),
          Expanded(child: Text(mdWeek(t.dateOf(_day)), style: sans(13, c: C.muted))),
          Text('${items.length}곳', style: mono(12, c: C.muted)),
        ]),
      ),
      Expanded(
        child: items.isEmpty
            ? ListView(padding: const EdgeInsets.fromLTRB(18, 4, 18, 18), children: const [
                EmptyNote('이날 일정이 비어 있음',
                    '"담은 곳"에서 꺼내 넣거나 "+ 추가"로 장소·항공편·숙소·쇼핑·맛집 카드를 넣어. 카드를 길게 눌러 끌면 순서가 바뀜.'),
              ])
            : ReorderableListView.builder(
                padding: const EdgeInsets.fromLTRB(18, 4, 18, 18),
                itemCount: items.length,
                buildDefaultDragHandles: false,
                proxyDecorator: (child, _, _) => Material(color: Colors.transparent, child: child),
                onReorderItem: (a, b) => store.reorder(t, _day, a, b),
                itemBuilder: (_, i) {
                  final it = items[i];
                  return ReorderableDelayedDragStartListener(
                    key: ValueKey(it.id),
                    index: i,
                    child: Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: SlideIn(
                        index: i,
                        child: ItemCard(
                          item: it,
                          number: i + 1,
                          cats: cat.cats,
                          onTap: () => Navigator.push(
                              context, slideRoute(ItemEditScreen(tripId: t.id, itemId: it.id, day: _day))),
                          onMaps: it.type == ItemType.memo || it.type == ItemType.flight
                              ? null
                              : () => openMaps(it, cityEn: cityEn),
                        ),
                      ),
                    ),
                  );
                },
              ),
      ),
      Padding(
        padding: const EdgeInsets.fromLTRB(18, 4, 18, 14),
        child: Row(children: [
          Expanded(
            flex: 10,
            child: PassButton('담은 곳 ${t.wish.length}', onTap: () => showWishSheet(context, t.id, defaultDay: _day)),
          ),
          const SizedBox(width: 10),
          Expanded(flex: 14, child: PassButton('+ 추가', primary: true, onTap: () => _addSheet(context, t))),
        ]),
      ),
    ]);
  }

  void _addSheet(BuildContext context, Trip t) {
    final leg = t.legOf(_day);
    showModalBottomSheet(
      context: context,
      builder: (c) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(18, 0, 18, 18),
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('D${_day + 1}에 넣기', style: sans(18, w: FontWeight.w800)),
            const SizedBox(height: 12),
            _addOption(c, Icons.travel_explore_rounded, '장소 찾기', '${leg?.cityKo ?? ''} 관광지에서 담기', () {
              Navigator.push(context, slideRoute(GuideScreen(tripId: t.id, destId: leg?.destId ?? '', day: _day)));
            }),
            GridView.count(
              crossAxisCount: 3,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: 10,
              crossAxisSpacing: 10,
              childAspectRatio: 1.25,
              padding: const EdgeInsets.only(top: 10),
              children: [
                for (final type in [ItemType.flight, ItemType.stay, ItemType.food, ItemType.shopping, ItemType.place, ItemType.memo])
                  _typeTile(c, type, () => Navigator.push(
                      context, slideRoute(ItemEditScreen(tripId: t.id, newType: type, day: _day)))),
              ],
            ),
          ]),
        ),
      ),
    );
  }

  Widget _addOption(BuildContext sheet, IconData icon, String title, String sub, VoidCallback onTap) => Press(
        onTap: () {
          Navigator.pop(sheet);
          onTap();
        },
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: C.ink,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(children: [
            Icon(icon, color: Colors.white),
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(title, style: sans(15, w: FontWeight.w800, c: Colors.white)),
                Text(sub, style: sans(12, c: C.onInk)),
              ]),
            ),
            const Icon(Icons.chevron_right_rounded, color: C.onInk),
          ]),
        ),
      );

  Widget _typeTile(BuildContext sheet, ItemType type, VoidCallback onTap) => Press(
        onTap: () {
          Navigator.pop(sheet);
          onTap();
        },
        child: Container(
          decoration: BoxDecoration(
            color: C.paper,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: C.ink, width: 1.5),
            boxShadow: const [BoxShadow(color: C.ink, offset: Offset(0, 3))],
          ),
          child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
            Icon(itemIcon(type), color: C.ink),
            const SizedBox(height: 4),
            Text(type == ItemType.place ? '직접 장소' : type.label, style: sans(13, w: FontWeight.w800)),
          ]),
        ),
      );
}

class _Tabs extends StatelessWidget {
  final List<String> labels;
  final int index;
  final ValueChanged<int> onTap;
  const _Tabs({required this.labels, required this.index, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 44,
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: C.line2, width: 1.5),
      ),
      child: LayoutBuilder(builder: (context, box) {
        final w = box.maxWidth / labels.length;
        return Stack(children: [
          AnimatedPositioned(
            duration: const Duration(milliseconds: 240),
            curve: Curves.easeOutBack,
            left: w * index,
            top: 0,
            bottom: 0,
            width: w,
            child: Container(decoration: BoxDecoration(color: C.ink, borderRadius: BorderRadius.circular(9))),
          ),
          Row(children: [
            for (var i = 0; i < labels.length; i++)
              Expanded(
                child: GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: () => onTap(i),
                  child: Center(
                    child: AnimatedDefaultTextStyle(
                      duration: const Duration(milliseconds: 200),
                      style: sans(14, w: FontWeight.w800, c: i == index ? Colors.white : C.muted),
                      child: Text(labels[i]),
                    ),
                  ),
                ),
              ),
          ]),
        ]);
      }),
    );
  }
}
