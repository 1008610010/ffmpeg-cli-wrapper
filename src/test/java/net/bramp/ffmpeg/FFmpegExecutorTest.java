package net.bramp.ffmpeg;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CountingOutputStream;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.fixtures.Samples;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.RecordingProgressListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests actually shelling out ffmpeg and ffprobe. Could be flakey if ffmpeg or ffprobe change.
 */
public class FFmpegExecutorTest {

  @Rule
  public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);

  final FFmpeg ffmpeg = new FFmpeg();
  final FFprobe ffprobe = new FFprobe();
  final FFmpegExecutor ffExecutor = new FFmpegExecutor(ffmpeg, ffprobe);

  final ExecutorService executor = Executors.newSingleThreadExecutor();

  public FFmpegExecutorTest() throws IOException {}

  @Test
  public void testTwoPass() throws InterruptedException, ExecutionException, IOException {
    FFmpegProbeResult in = ffprobe.probe(Samples.big_buck_bunny_720p_1mb);
    assertFalse(in.hasError());

    // @formatter:off
    FFmpegBuilder builder = new FFmpegBuilder()
      .setInput(in)
      .overrideOutputFiles(true)
      .addOutput(Samples.output_mp4)
        .setFormat("mp4")
        .disableAudio()
        .setVideoCodec("mpeg4")
        .setVideoFrameRate(FFmpeg.FPS_30)
        .setVideoResolution(320, 240)
        .setTargetSize(1024 * 1024)
        .done();
    // @formatter:on


    FFmpegJob job = ffExecutor.createTwoPassJob(builder);
    runAndWait(job);

    assertEquals(FFmpegJob.State.FINISHED, job.getState());
  }

  @Test
  public void testFilter() throws InterruptedException, ExecutionException, IOException {

    // @formatter:off
    FFmpegBuilder builder = new FFmpegBuilder()
      .setInput(Samples.big_buck_bunny_720p_1mb)
      .overrideOutputFiles(true)
      .addOutput(Samples.output_mp4)
        .setFormat("mp4")
        .disableAudio()
        .setVideoCodec("mpeg4")
        .setVideoFilter("scale=320:trunc(ow/a/2)*2")
        .done();
    // @formatter:on

    FFmpegJob job = ffExecutor.createJob(builder);
    runAndWait(job);

    assertEquals(FFmpegJob.State.FINISHED, job.getState());
  }

  @Test
  public void testMetaTags() throws InterruptedException, ExecutionException, IOException {

    // @formatter:off
    FFmpegBuilder builder = new FFmpegBuilder()
      .setInput(Samples.big_buck_bunny_720p_1mb)
      .overrideOutputFiles(true)
      .addOutput(Samples.output_mp4)
        .setFormat("mp4")
        .disableAudio()
        .setVideoCodec("mpeg4")
        .addMetaTag("comment", "This=Nice!")
        .addMetaTag("title", "Big Buck Bunny")
        .done();
    // @formatter:on

    FFmpegJob job = ffExecutor.createJob(builder);
    runAndWait(job);

    assertEquals(FFmpegJob.State.FINISHED, job.getState());
  }

  /**
   * Test if addStdoutOutput() actually works, and the output can be correctly captured.
   *
   * @throws InterruptedException
   * @throws ExecutionException
   * @throws IOException
   */
  @Test
  public void testStdout() throws InterruptedException, ExecutionException, IOException {

    // @formatter:off
    FFmpegBuilder builder = new FFmpegBuilder()
      .setInput(Samples.big_buck_bunny_720p_1mb)
      .addStdoutOutput()
        .setFormat("s8")
        .setAudioChannels(1)
        .done();

    List<String> newArgs = ImmutableList.<String>builder()
      .add(ffmpeg.getPath())
      .addAll(builder.build())
      .build();
    // @formatter:on

    // TODO Add support to the FFmpegJob to export the stream
    Process p = new ProcessBuilder(newArgs).start();

    CountingOutputStream out = new CountingOutputStream(ByteStreams.nullOutputStream());
    ByteStreams.copy(p.getInputStream(), out);

    assertEquals(0, p.waitFor());

    // This is perhaps fragile, but one byte per audio sample
    assertEquals(254976, out.getCount());
  }

  @Test
  public void testProgress() throws InterruptedException, ExecutionException, IOException {
    FFmpegProbeResult in = ffprobe.probe(Samples.big_buck_bunny_720p_1mb);

    assertFalse(in.hasError());

    // @formatter:off
    FFmpegBuilder builder = new FFmpegBuilder()
      .readAtNativeFrameRate() // Slows the test down
      .setInput(in)
      .overrideOutputFiles(true)
      .addOutput(Samples.output_mp4)
      .done();
    // @formatter:on

    RecordingProgressListener listener = new RecordingProgressListener();

    FFmpegJob job = ffExecutor.createJob(builder, listener);
    runAndWait(job);

    assertEquals(FFmpegJob.State.FINISHED, job.getState());

    List<Progress> progesses = listener.progesses;

    // Since the results of ffmpeg are not predictable, test for the bare minimum.
    assertThat(progesses, hasSize(greaterThanOrEqualTo(2)));
    assertThat(progesses.get(0).progress, is("continue"));
    assertThat(progesses.get(progesses.size() - 1).progress, is("end"));
  }

  protected void runAndWait(FFmpegJob job) throws ExecutionException, InterruptedException {
    executor.submit(job).get();
  }
}
