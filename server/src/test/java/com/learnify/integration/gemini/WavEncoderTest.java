package com.learnify.integration.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class WavEncoderTest {

    @Test
    void wrapsPcmWithA44ByteRiffWaveHeader() {
        byte[] pcm = new byte[100];

        byte[] wav = WavEncoder.wrapPcmAsWav(pcm, 24_000, 1, 16);

        assertThat(wav).hasSize(44 + pcm.length);
        assertThat(new String(wav, 0, 4)).isEqualTo("RIFF");
        assertThat(new String(wav, 8, 4)).isEqualTo("WAVE");
        assertThat(new String(wav, 12, 4)).isEqualTo("fmt ");
        assertThat(new String(wav, 36, 4)).isEqualTo("data");

        ByteBuffer buffer = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        assertThat(buffer.getInt(4)).isEqualTo(36 + pcm.length); // RIFF chunk size
        assertThat(buffer.getShort(22)).isEqualTo((short) 1); // channels
        assertThat(buffer.getInt(24)).isEqualTo(24_000); // sample rate
        assertThat(buffer.getShort(34)).isEqualTo((short) 16); // bits per sample
        assertThat(buffer.getInt(40)).isEqualTo(pcm.length); // data chunk size
    }
}
