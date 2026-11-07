// 체크리스트: 날씨(기온·비)와 레저 활동에 따라 항목이 바뀜
import 'package:flutter/material.dart';

import '../data/checklist_templates.dart';
import '../data/models.dart';
import '../data/store.dart';
import '../ui/theme.dart';

class ChecklistTab extends StatefulWidget {
  final Trip trip;
  const ChecklistTab({super.key, required this.trip});
  @override
  State<ChecklistTab> createState() => _ChecklistTabState();
}

class _ChecklistTabState extends State<ChecklistTab> {
  bool _hideDone = false;

  void _apply(Store s, void Function() change) {
    change();
    syncChecklist(widget.trip);
    s.changed();
  }

  Future<void> _addCustom(Store s) async {
    final text = TextEditingController();
    final groups = {...groupOrder, ...widget.trip.checklist.map((c) => c.group)}.toList();
    var group = '생활';
    final ok = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (c) => StatefulBuilder(
        builder: (c, set) => Padding(
          padding: EdgeInsets.fromLTRB(18, 0, 18, 18 + MediaQuery.of(c).viewInsets.bottom),
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('항목 추가', style: sans(18, w: FontWeight.w800)),
            const SizedBox(height: 12),
            TextField(controller: text, autofocus: true, style: sans(15), decoration: const InputDecoration(hintText: '예: 국제운전면허증')),
            const SizedBox(height: 12),
            Wrap(spacing: 6, runSpacing: 6, children: [
              for (final g in groups) PassChip(g, selected: g == group, onTap: () => set(() => group = g)),
            ]),
            const SizedBox(height: 16),
            PassButton('추가', primary: true, onTap: () => Navigator.pop(c, true)),
          ]),
        ),
      ),
    );
    if (ok == true && text.text.trim().isNotEmpty) {
      widget.trip.checklist.add(CheckItem(text: text.text.trim(), group: group, src: 'custom'));
      s.changed();
    }
    text.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = StoreScope.of(context);
    final t = widget.trip;
    final done = t.checklist.where((c) => c.done).length;
    final total = t.checklist.length;
    final groups = <String, List<CheckItem>>{};
    for (final c in t.checklist) {
      if (_hideDone && c.done) continue;
      groups.putIfAbsent(c.group, () => []).add(c);
    }
    final order = [...groupOrder.where(groups.containsKey), ...groups.keys.where((g) => !groupOrder.contains(g))];

    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 12, 18, 24),
      children: [
        // 진행률
        Row(children: [
          Text('$done/$total', style: mono(22, bold: true)),
          const SizedBox(width: 10),
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: TweenAnimationBuilder<double>(
                tween: Tween(end: total == 0 ? 0 : done / total),
                duration: const Duration(milliseconds: 400),
                builder: (_, v, _) => LinearProgressIndicator(value: v, minHeight: 8, color: C.ink, backgroundColor: C.line),
              ),
            ),
          ),
          const SizedBox(width: 6),
          TextButton(
            onPressed: () => setState(() => _hideDone = !_hideDone),
            child: Text(_hideDone ? '모두 보기' : '챙긴 것 숨기기', style: sans(12.5, c: C.muted)),
          ),
        ]),
        const SizedBox(height: 14),
        // 날씨
        Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(color: C.paper, borderRadius: BorderRadius.circular(14), border: Border.all(color: C.line, width: 1.5)),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              const Eyebrow('WEATHER'),
              const SizedBox(width: 8),
              Expanded(
                child: Text(t.climateManual ? '직접 고름' : '출발 월·위치로 추정 · 바꿔도 됨',
                    style: sans(11.5, c: C.faint), overflow: TextOverflow.ellipsis),
              ),
            ]),
            const SizedBox(height: 8),
            Row(children: [
              for (final c in Climate.values) ...[
                Expanded(
                  child: Press(
                    onTap: () => _apply(s, () {
                      t.climate = c;
                      t.climateManual = true;
                    }),
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 180),
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      decoration: BoxDecoration(
                        color: t.climate == c ? C.ink : Colors.transparent,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(color: t.climate == c ? C.ink : C.line2, width: 1.5),
                      ),
                      child: Column(children: [
                        Text(c.label, style: sans(14, w: FontWeight.w800, c: t.climate == c ? Colors.white : C.ink)),
                        Text(c.range, style: mono(10, c: t.climate == c ? C.onInk : C.faint)),
                      ]),
                    ),
                  ),
                ),
                if (c != Climate.values.last) const SizedBox(width: 6),
              ],
            ]),
            const SizedBox(height: 8),
            PassChip(t.rainy ? '☂ 비 소식 있음' : '비 소식 없음',
                selected: t.rainy, onTap: () => _apply(s, () => t.rainy = !t.rainy)),
          ]),
        ),
        const SizedBox(height: 10),
        // 레저
        Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(color: C.paper, borderRadius: BorderRadius.circular(14), border: Border.all(color: C.line, width: 1.5)),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Eyebrow('ACTIVITIES · 고르면 준비물이 추가됨'),
            const SizedBox(height: 8),
            Wrap(spacing: 6, runSpacing: 6, children: [
              for (final a in activityTemplates)
                PassChip(a.label,
                    selected: t.activities.contains(a.key),
                    selectedColor: C.red,
                    onTap: () => _apply(s, () {
                          if (!t.activities.remove(a.key)) t.activities.add(a.key);
                        })),
            ]),
          ]),
        ),
        const SizedBox(height: 16),
        for (final g in order) ...[
          Padding(
            padding: const EdgeInsets.only(top: 6, bottom: 6),
            child: Row(children: [
              Text(g, style: sans(14, w: FontWeight.w800)),
              const SizedBox(width: 6),
              Text('${groups[g]!.where((c) => c.done).length}/${groups[g]!.length}', style: mono(11, c: C.faint)),
            ]),
          ),
          Container(
            decoration: BoxDecoration(
              color: C.paper,
              borderRadius: BorderRadius.circular(14),
              boxShadow: const [BoxShadow(color: Color(0x1214213D), blurRadius: 10, offset: Offset(0, 4))],
            ),
            child: Column(children: [
              for (final c in groups[g]!) _row(s, c, c == groups[g]!.last),
            ]),
          ),
          const SizedBox(height: 8),
        ],
        const SizedBox(height: 6),
        PassButton('+ 항목 직접 추가', dashed: true, onTap: () => _addCustom(s)),
      ],
    );
  }

  Widget _row(Store s, CheckItem c, bool last) {
    return Dismissible(
      key: ValueKey(c.id),
      direction: DismissDirection.endToStart,
      background: Container(
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.only(right: 18),
        decoration: BoxDecoration(color: C.red, borderRadius: BorderRadius.circular(14)),
        child: const Icon(Icons.delete_outline_rounded, color: Colors.white),
      ),
      onDismissed: (_) {
        hideCheck(widget.trip, c);
        s.changed();
      },
      child: InkWell(
        onTap: () {
          c.done = !c.done;
          s.changed();
        },
        child: Container(
          constraints: const BoxConstraints(minHeight: 48),
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(border: last ? null : const Border(bottom: BorderSide(color: Color(0xFFEDF0F4)))),
          child: Row(children: [
            AnimatedContainer(
              duration: const Duration(milliseconds: 180),
              width: 22,
              height: 22,
              decoration: BoxDecoration(
                color: c.done ? C.ink : Colors.transparent,
                borderRadius: BorderRadius.circular(6),
                border: Border.all(color: c.done ? C.ink : C.line2, width: 1.5),
              ),
              child: c.done ? const Icon(Icons.check_rounded, size: 16, color: Colors.white) : null,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: AnimatedDefaultTextStyle(
                duration: const Duration(milliseconds: 180),
                style: sans(14.5,
                        w: FontWeight.w600, c: c.done ? C.faint : C.ink)
                    .copyWith(decoration: c.done ? TextDecoration.lineThrough : null),
                child: Text(c.text),
              ),
            ),
            if (c.src == 'custom') Text('직접', style: mono(10, c: C.faint)),
          ]),
        ),
      ),
    );
  }
}
