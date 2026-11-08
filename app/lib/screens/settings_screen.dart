// 백업·복원 (서버가 없으니 폰 교체·재설치 전에 파일로 저장)
import 'dart:convert';
import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:share_plus/share_plus.dart';

import '../data/store.dart';
import '../ui/theme.dart';
import '../util/format.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  String _fileName() {
    final n = DateTime.now();
    return 'travle-backup-${n.year}${two(n.month)}${two(n.day)}-${two(n.hour)}${two(n.minute)}.json';
  }

  Future<void> _save(BuildContext context, Store s) async {
    final bytes = Uint8List.fromList(utf8.encode(s.exportJson()));
    try {
      final uri = await FilePicker.saveFile(fileName: _fileName(), bytes: bytes, mimeType: 'application/json');
      if (uri != null && context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('백업 파일을 저장했음')));
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('저장하지 못함: $e')));
      }
    }
  }

  Future<void> _share(BuildContext context, Store s) async {
    final bytes = Uint8List.fromList(utf8.encode(s.exportJson()));
    await SharePlus.instance.share(
      ShareParams(
        files: [XFile.fromData(bytes, mimeType: 'application/json', name: _fileName())],
        fileNameOverrides: [_fileName()],
        subject: '트래블 백업',
      ),
    );
  }

  Future<void> _restore(BuildContext context, Store s) async {
    final f = await FilePicker.pickFile(type: FileType.any);
    if (f == null || !context.mounted) return;
    String text;
    int count;
    try {
      text = utf8.decode(await f.readAsBytes());
      final j = jsonDecode(text) as Map<String, dynamic>;
      count = (j['trips'] as List).length;
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('트래블 백업 파일이 아님')));
      }
      return;
    }
    if (!context.mounted) return;
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('백업으로 바꾸기'),
        content: Text('지금 폰에 있는 여행 ${s.trips.length}개가 지워지고 백업의 여행 $count개로 바뀜.', style: sans(14, c: C.muted)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('취소')),
          TextButton(
            onPressed: () => Navigator.pop(c, true),
            child: Text(
              '바꾸기',
              style: sans(14, c: C.red, w: FontWeight.w800),
            ),
          ),
        ],
      ),
    );
    if (ok == true) {
      try {
        s.importJson(text);
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('여행 $count개를 불러왔음')));
        }
      } catch (_) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('백업 파일이 손상돼서 불러오지 못함. 지금 데이터는 그대로임')));
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = StoreScope.of(context);
    final cat = s.catalog!;
    final places = cat.places.values.fold(0, (a, l) => a + l.length);
    return Scaffold(
      appBar: AppBar(
        title: Text('백업·설정', style: sans(18, w: FontWeight.w800)),
      ),
      body: SafeArea(
        top: false,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(18, 8, 18, 24),
          children: [
            const EmptyNote('백업이 필요한 이유', '이 앱은 서버 없이 폰 안에만 저장함. 앱을 지우거나 폰을 바꾸기 전에 백업 파일을 저장해두고, 새로 깐 뒤 불러오면 그대로 돌아옴.'),
            const SizedBox(height: 16),
            PassButton('백업 파일 저장', icon: Icons.save_alt_rounded, primary: true, onTap: () => _save(context, s)),
            const SizedBox(height: 10),
            PassButton('백업 파일 공유 (카톡·드라이브)', icon: Icons.ios_share_rounded, onTap: () => _share(context, s)),
            const SizedBox(height: 10),
            PassButton('백업 불러오기', icon: Icons.restore_rounded, dashed: true, onTap: () => _restore(context, s)),
            const SizedBox(height: 28),
            const Eyebrow('DATA'),
            const SizedBox(height: 8),
            _kv('내 여행', '${s.trips.length}개'),
            _kv('내장 여행지', '${comma(cat.dests.length)}곳'),
            _kv('내장 관광 장소', '${comma(places)}곳'),
            _kv('장소 데이터 출처', 'Wikidata · Wikipedia'),
            _kv('지도·길안내', '구글지도 앱으로 열기'),
          ],
        ),
      ),
    );
  }

  Widget _kv(String k, String v) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 8),
    child: Row(
      children: [
        Text(k, style: sans(14, c: C.muted)),
        const Spacer(),
        Text(v, style: sans(14, w: FontWeight.w800)),
      ],
    ),
  );
}
