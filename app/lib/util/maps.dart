// 구글지도 앱으로 열기 (API 키 없이 링크만 사용)
import 'package:url_launcher/url_launcher.dart';

import '../data/models.dart';

Uri mapsUri(TripItem item, {String cityEn = ''}) {
  final link = item.get('maps');
  if (link.startsWith('http')) return Uri.parse(link);
  final address = item.get('address');
  final en = item.get('en');
  // 이름으로 검색해야 구글지도에서 장소 페이지(리뷰·영업시간)가 바로 열림
  final q = address.isNotEmpty
      ? '${item.title} $address'
      : [en.isNotEmpty ? en : item.title, if (cityEn.isNotEmpty) cityEn].join(' ');
  return Uri.https('www.google.com', '/maps/search/', {'api': '1', 'query': q});
}

Future<bool> openMaps(TripItem item, {String cityEn = ''}) =>
    launchUrl(mapsUri(item, cityEn: cityEn), mode: LaunchMode.externalApplication);
