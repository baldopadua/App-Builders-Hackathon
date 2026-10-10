import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../main.dart';
import '../services/platform_bridge.dart';

class SecretsScreen extends StatefulWidget {
  const SecretsScreen({super.key});

  @override
  State<SecretsScreen> createState() => _SecretsScreenState();
}

class _SecretsScreenState extends State<SecretsScreen> {
  final TextEditingController _labelController = TextEditingController();
  final TextEditingController _valueController = TextEditingController();
  final FocusNode _labelFocus = FocusNode();
  final FocusNode _valueFocus = FocusNode();

  List<PrivateSecret> _secrets = [];
  bool _isLoading = false;
  bool _isSaving = false;

  static const int _maxNotes = 5;

  @override
  void initState() {
    super.initState();
    _loadSecrets();
  }

  @override
  void dispose() {
    _labelController.dispose();
    _valueController.dispose();
    _labelFocus.dispose();
    _valueFocus.dispose();
    super.dispose();
  }

  Future<void> _loadSecrets() async {
    setState(() => _isLoading = true);
    try {
      final secrets = await PlatformBridge.getSecrets();
      if (mounted) {
        setState(() {
          _secrets = secrets;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _onSaveSecret() async {
    if (_secrets.length >= _maxNotes) {
      _showFeedback(
        'You have $_maxNotes notes. Delete one to add another.',
        isError: true,
      );
      return;
    }

    final label = _labelController.text.trim();
    final value = _valueController.text.trim();

    if (label.isEmpty || value.isEmpty) {
      _showFeedback(
        'Please enter both a note label and the private details.',
        isError: true,
      );
      return;
    }

    setState(() => _isSaving = true);
    try {
      final created = await PlatformBridge.addSecret(label: label, value: value);
      if (mounted) {
        setState(() => _isSaving = false);
        if (created != null) {
          _labelController.clear();
          _valueController.clear();
          FocusScope.of(context).unfocus();
          _loadSecrets();
          _showFeedback('Private note saved.');
        } else {
          _showFeedback('Could not save note. Please try again.', isError: true);
        }
      }
    } catch (_) {
      if (mounted) {
        setState(() => _isSaving = false);
        _showFeedback('Error saving note. Please retry.', isError: true);
      }
    }
  }

  void _showFeedback(String message, {bool isError = false}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          message,
          style: GoogleFonts.plusJakartaSans(fontSize: 13.5),
        ),
        backgroundColor: isError ? AppTokens.redText : AppTokens.brandPurple,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  Future<void> _confirmDeleteNote(PrivateSecret secret) async {
    final shouldDelete = await showDialog<bool>(
      context: context,
      builder: (context) {
        return Dialog(
          backgroundColor: AppTokens.surfaceWhite,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 24, 20, 20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Delete this note?',
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: AppTokens.textPrimary,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Tanggolin will stop watching for "${secret.label}" in your drafts.',
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 14,
                    height: 1.45,
                    color: AppTokens.textSecondary,
                  ),
                ),
                const SizedBox(height: 20),
                Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    TextButton(
                      onPressed: () => Navigator.pop(context, false),
                      style: TextButton.styleFrom(
                        foregroundColor: AppTokens.textSecondary,
                      ),
                      child: Text(
                        'Cancel',
                        style: GoogleFonts.plusJakartaSans(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    ElevatedButton(
                      onPressed: () => Navigator.pop(context, true),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppTokens.redText,
                        foregroundColor: Colors.white,
                        minimumSize: const Size(80, 42),
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                      ),
                      child: Text(
                        'Delete',
                        style: GoogleFonts.plusJakartaSans(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );

    if (shouldDelete == true) {
      await PlatformBridge.deleteSecret(secret.id);
      _loadSecrets();
      _showFeedback('Note removed.');
    }
  }

  void _showNoteDetailModal(PrivateSecret secret) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (ctx) => Container(
        padding: const EdgeInsets.fromLTRB(24, 20, 24, 32),
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: const Color(0xFFE2E8F0),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 18),
            Text(
              secret.label,
              style: GoogleFonts.plusJakartaSans(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: AppTokens.textPrimary,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              'Stored on this device, encrypted with AES-GCM.',
              style: GoogleFonts.plusJakartaSans(
                fontSize: 13.5,
                color: AppTokens.textSecondary,
              ),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: OutlinedButton.icon(
                onPressed: () {
                  Navigator.pop(ctx);
                  _confirmDeleteNote(secret);
                },
                icon: const Icon(Icons.delete_outline_rounded,
                    size: 18, color: AppTokens.redText),
                label: Text(
                  'Delete note',
                  style: GoogleFonts.plusJakartaSans(
                    fontWeight: FontWeight.w600,
                    color: AppTokens.redText,
                  ),
                ),
                style: OutlinedButton.styleFrom(
                  side: const BorderSide(color: AppTokens.redBorder),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isFull = _secrets.length >= _maxNotes;

    return Scaffold(
      backgroundColor: AppTokens.surfaceWhite,
      appBar: AppBar(
        titleSpacing: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, size: 18),
          color: AppTokens.textPrimary,
          onPressed: () => Navigator.pop(context),
        ),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Private notes',
              style: GoogleFonts.plusJakartaSans(
                fontSize: 19,
                fontWeight: FontWeight.w700,
                color: AppTokens.textPrimary,
                letterSpacing: -0.3,
              ),
            ),
            Text(
              'Teach Tanggolin what to watch for.',
              style: GoogleFonts.plusJakartaSans(
                fontSize: 12.5,
                fontWeight: FontWeight.w400,
                color: AppTokens.textSecondary,
              ),
            ),
          ],
        ),
      ),
      body: SafeArea(
        child: _isLoading
            ? const Center(
                child: CircularProgressIndicator(color: AppTokens.brandPurple),
              )
            : SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Limit reached banner if 5/5
                    if (isFull) ...[
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 14, vertical: 10),
                        decoration: BoxDecoration(
                          color: AppTokens.surfaceTinted,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: AppTokens.borderLavender),
                        ),
                        child: Row(
                          children: [
                            const Icon(
                              Icons.info_outline_rounded,
                              size: 18,
                              color: AppTokens.brandPurple,
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Text(
                                'You have $_maxNotes notes. Delete one to add another.',
                                style: GoogleFonts.plusJakartaSans(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w500,
                                  color: AppTokens.brandPurple,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 18),
                    ],

                    // Direct Form on White Surface (No bulky lavender card)
                    // Note label
                    Text(
                      'Note label',
                      style: GoogleFonts.plusJakartaSans(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: AppTokens.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 6),
                    TextField(
                      controller: _labelController,
                      focusNode: _labelFocus,
                      enabled: !isFull && !_isSaving,
                      textInputAction: TextInputAction.next,
                      decoration: const InputDecoration(
                        hintText: 'Project nickname',
                      ),
                      style: GoogleFonts.plusJakartaSans(
                        fontSize: 15,
                        color: AppTokens.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Choose a label you can recognize later.',
                      style: GoogleFonts.plusJakartaSans(
                        fontSize: 12.5,
                        color: AppTokens.textSecondary,
                      ),
                    ),

                    const SizedBox(height: 18),

                    // Private details
                    Text(
                      'Private details',
                      style: GoogleFonts.plusJakartaSans(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: AppTokens.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 6),
                    TextField(
                      controller: _valueController,
                      focusNode: _valueFocus,
                      enabled: !isFull && !_isSaving,
                      maxLines: null,
                      minLines: 3,
                      decoration: const InputDecoration(
                        hintText: 'The phrase or details to flag',
                        alignLabelWithHint: true,
                      ),
                      style: GoogleFonts.plusJakartaSans(
                        fontSize: 15,
                        color: AppTokens.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Saved on this device, encrypted.',
                      style: GoogleFonts.plusJakartaSans(
                        fontSize: 12.5,
                        color: AppTokens.textSecondary,
                      ),
                    ),

                    const SizedBox(height: 20),

                    // Save private note button
                    SizedBox(
                      width: double.infinity,
                      height: 50,
                      child: ElevatedButton(
                        onPressed: (isFull || _isSaving) ? null : _onSaveSecret,
                        child: _isSaving
                            ? const SizedBox(
                                width: 22,
                                height: 22,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2.2,
                                  color: Colors.white,
                                ),
                              )
                            : const Text('Save private note'),
                      ),
                    ),

                    const SizedBox(height: 36),

                    // Saved notes Section Header with quiet count
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      crossAxisAlignment: CrossAxisAlignment.center,
                      children: [
                        Text(
                          'Saved notes',
                          style: GoogleFonts.plusJakartaSans(
                            fontSize: 17,
                            fontWeight: FontWeight.w700,
                            color: AppTokens.textPrimary,
                            letterSpacing: -0.2,
                          ),
                        ),
                        Text(
                          '${_secrets.length} of $_maxNotes saved',
                          style: GoogleFonts.plusJakartaSans(
                            fontSize: 13,
                            fontWeight: FontWeight.w500,
                            color: AppTokens.textSecondary,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),

                    // Notes List or Empty State
                    if (_secrets.isEmpty)
                      _buildEmptyState()
                    else
                      ListView.separated(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        itemCount: _secrets.length,
                        separatorBuilder: (context, index) => const Divider(
                          color: Color(0xFFF1F5F9),
                          height: 1,
                        ),
                        itemBuilder: (context, index) {
                          final secret = _secrets[index];
                          return InkWell(
                            onTap: () => _showNoteDetailModal(secret),
                            borderRadius: BorderRadius.circular(8),
                            child: Padding(
                              padding: const EdgeInsets.symmetric(
                                  vertical: 12, horizontal: 4),
                              child: Row(
                                children: [
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          secret.label,
                                          style: GoogleFonts.plusJakartaSans(
                                            fontSize: 15,
                                            fontWeight: FontWeight.w600,
                                            color: AppTokens.textPrimary,
                                          ),
                                        ),
                                        const SizedBox(height: 2),
                                        Text(
                                          'Stored on this device',
                                          style: GoogleFonts.plusJakartaSans(
                                            fontSize: 12.5,
                                            color: AppTokens.textSecondary,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                  IconButton(
                                    icon: const Icon(
                                      Icons.delete_outline_rounded,
                                      size: 20,
                                      color: AppTokens.textSecondary,
                                    ),
                                    tooltip: 'Delete note',
                                    onPressed: () => _confirmDeleteNote(secret),
                                  ),
                                ],
                              ),
                            ),
                          );
                        },
                      ),
                  ],
                ),
              ),
      ),
    );
  }

  /// Empty state with small pangolin silhouette and calm guidance
  Widget _buildEmptyState() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 36),
      child: Center(
        child: Column(
          children: [
            Container(
              width: 56,
              height: 56,
              decoration: const BoxDecoration(
                color: AppTokens.lavenderSelected,
                shape: BoxShape.circle,
              ),
              padding: const EdgeInsets.all(12),
              child: Image.asset(
                'assets/icons/tanggolin_icon.png',
                fit: BoxFit.contain,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              'No private notes yet',
              style: GoogleFonts.plusJakartaSans(
                fontSize: 15,
                fontWeight: FontWeight.w600,
                color: AppTokens.textPrimary,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              'Add a phrase or detail you want Tanggolin to watch for.',
              textAlign: TextAlign.center,
              style: GoogleFonts.plusJakartaSans(
                fontSize: 13,
                color: AppTokens.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
