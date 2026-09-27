-- Cache the generated Hinglish translation + synthesized audio per lesson so
-- repeat "Listen in Hinglish" clicks don't re-spend two Gemini calls (translation
-- + TTS) on content that never changes once a lesson is generated.

alter table lesson add column hinglish_text text;
alter table lesson add column hinglish_audio bytea;
