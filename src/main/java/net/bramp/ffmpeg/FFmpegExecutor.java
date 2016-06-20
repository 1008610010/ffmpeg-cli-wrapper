package net.bramp.ffmpeg;

import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.job.SinglePassFFmpegJob;
import net.bramp.ffmpeg.job.TwoPassFFmpegJob;
import net.bramp.ffmpeg.progress.FFmpegProgressListener;

import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public class FFmpegExecutor {

  final FFmpeg ffmpeg;
  final FFprobe ffprobe;

  public FFmpegExecutor(FFmpeg ffmpeg, FFprobe ffprobe) {
    this.ffmpeg = checkNotNull(ffmpeg);
    this.ffprobe = checkNotNull(ffprobe);
  }

  public FFmpegJob createJob(FFmpegBuilder builder) {
    // Single Pass
    return new SinglePassFFmpegJob(ffmpeg, builder);
  }

  public FFmpegJob createJob(FFmpegBuilder builder, FFmpegProgressListener listener) {
    // Single Pass
    return new SinglePassFFmpegJob(ffmpeg, builder);
  }

  /**
   * Info: https://trac.ffmpeg.org/wiki/x264EncodingGuide#twopass
   * 
   * @param builder
   * @return A new two-pass FFmpegJob
   */
  public FFmpegJob createTwoPassJob(FFmpegBuilder builder) {
    return new TwoPassFFmpegJob(ffmpeg, builder);
  }
}
