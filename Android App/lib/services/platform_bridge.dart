import 'package:flutter/services.dart';

enum DetectionRisk {
  high,
  medium,
}

class DetectionResult {
  final DetectionRisk risk;
  final String headline;
  final String tip;
  final String category;

  DetectionResult({
    required this.risk,
    required this.headline,
    required this.tip,
    required this.category,
  });

  factory DetectionResult.fromMap(Map<dynamic, dynamic> map) {
    final typeStr = map['type']?.toString().toUpperCase() ?? 'HIGH';
    return DetectionResult(
      risk: typeStr == 'MEDIUM' ? DetectionRisk.medium : DetectionRisk.high,
      headline: map['headline']?.toString() ?? '',
      tip: map['tip']?.toString() ?? '',
      category: map['category']?.toString() ?? 'Protection',
    );
  }
}

class PrivateSecret {
  final String id;
  final String label;
  final String value;
  final int createdAt;

  PrivateSecret({
    required this.id,
    required this.label,
    required this.value,
    required this.createdAt,
  });

  factory PrivateSecret.fromMap(Map<dynamic, dynamic> map) {
    return PrivateSecret(
      id: map['id']?.toString() ?? '',
      label: map['label']?.toString() ?? '',
      value: map['value']?.toString() ?? '',
      createdAt: map['createdAt'] is int ? map['createdAt'] as int : 0,
    );
  }
}

class PlatformBridge {
  static const MethodChannel _channel = MethodChannel('secondlook/ai');

  /// Test connection with native Android MethodChannel
  static Future<String> ping() async {
    try {
      final String? result = await _channel.invokeMethod<String>('ping');
      return result ?? 'no response';
    } on PlatformException catch (e) {
      return 'Error: ${e.message}';
    }
  }

  /// Real-time detection analysis for draft text
  static Future<DetectionResult?> check(String text) async {
    try {
      final Map<dynamic, dynamic>? result =
          await _channel.invokeMapMethod<dynamic, dynamic>('check', {'text': text});
      if (result != null) {
        return DetectionResult.fromMap(result);
      }
      return null;
    } on PlatformException catch (_) {
      return null;
    }
  }

  /// Masks sensitive content in text using the native engine
  static Future<String> hide(String text) async {
    try {
      final String? result =
          await _channel.invokeMethod<String>('hide', {'text': text});
      return result ?? text;
    } on PlatformException catch (_) {
      return text;
    }
  }

  /// Saves a new encrypted private note into the Keystore-backed SecretManager
  static Future<PrivateSecret?> addSecret({
    required String label,
    required String value,
  }) async {
    try {
      final Map<dynamic, dynamic>? result =
          await _channel.invokeMapMethod<dynamic, dynamic>('addSecret', {
        'label': label,
        'value': value,
      });
      if (result != null) {
        return PrivateSecret.fromMap(result);
      }
      return null;
    } on PlatformException catch (_) {
      return null;
    }
  }

  /// Retrieves all encrypted private notes saved by the user
  static Future<List<PrivateSecret>> getSecrets() async {
    try {
      final List<dynamic>? list =
          await _channel.invokeListMethod<dynamic>('getSecrets');
      if (list != null) {
        return list
            .whereType<Map<dynamic, dynamic>>()
            .map((item) => PrivateSecret.fromMap(item))
            .toList();
      }
      return [];
    } on PlatformException catch (_) {
      return [];
    }
  }

  /// Deletes an encrypted private note by its ID
  static Future<bool> deleteSecret(String id) async {
    try {
      final bool? success =
          await _channel.invokeMethod<bool>('deleteSecret', {'id': id});
      return success ?? false;
    } on PlatformException catch (_) {
      return false;
    }
  }

  /// Opens the Android system Input Method / Keyboard settings page
  static Future<bool> openInputMethodSettings() async {
    try {
      final bool? result =
          await _channel.invokeMethod<bool>('openInputMethodSettings');
      return result ?? true;
    } on PlatformException catch (_) {
      return false;
    }
  }

  /// Triggers the Android system Input Method Picker dialog
  static Future<bool> showInputMethodPicker() async {
    try {
      final bool? result =
          await _channel.invokeMethod<bool>('showInputMethodPicker');
      return result ?? true;
    } on PlatformException catch (_) {
      return false;
    }
  }

  /// Checks if Tanggolin / SecondLook keyboard is enabled in Android settings
  static Future<bool> isKeyboardEnabled() async {
    try {
      final bool? result =
          await _channel.invokeMethod<bool>('isKeyboardEnabled');
      return result ?? false;
    } on PlatformException catch (_) {
      return false;
    }
  }

  /// Checks if Tanggolin / SecondLook keyboard is selected as the default input method
  static Future<bool> isKeyboardSelected() async {
    try {
      final bool? result =
          await _channel.invokeMethod<bool>('isKeyboardSelected');
      return result ?? false;
    } on PlatformException catch (_) {
      return false;
    }
  }

  /// Checks whether Ingat Mode (2-second press-and-hold send confirmation) is enabled
  static Future<bool> isIngatModeEnabled() async {
    try {
      final bool? result =
          await _channel.invokeMethod<bool>('isIngatModeEnabled');
      return result ?? true;
    } on PlatformException catch (_) {
      return true;
    }
  }

  /// Toggles Ingat Mode (2-second press-and-hold send confirmation)
  static Future<bool> setIngatModeEnabled(bool enabled) async {
    try {
      final bool? result = await _channel.invokeMethod<bool>(
          'setIngatModeEnabled', {'enabled': enabled});
      return result ?? true;
    } on PlatformException catch (_) {
      return false;
    }
  }
}
