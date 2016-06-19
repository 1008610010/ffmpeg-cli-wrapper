package net.bramp.ffmpeg.job;

import com.google.common.base.Throwables;
import net.bramp.ffmpeg.FFmpeg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TwoPassFFmpegJob extends FFmpegJob {

  final static Logger LOG = LoggerFactory.getLogger(TwoPassFFmpegJob.class);

  final String passlogPrefix;
  final List<String> args1;
  final List<String> args2;

  public TwoPassFFmpegJob(FFmpeg ffmpeg, String passlogPrefix, List<String> args1,
      List<String> args2) {
    super(ffmpeg);
    this.passlogPrefix = passlogPrefix;
    this.args1 = args1;
    this.args2 = args2;
  }

  protected void deletePassLog() throws IOException {
    final Path cwd = Paths.get("");
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(cwd, passlogPrefix + "*.log*")) {
      for (Path p : stream) {
        Files.deleteIfExists(p);
      }
    }
  }

  public void run() {
    state = State.RUNNING;

    try {
      ffmpeg.run(args1);
      ffmpeg.run(args2);

      deletePassLog();

      state = State.FINISHED;

    } catch (Throwable t) {
      state = State.FAILED;
      Throwables.propagate(t);
    }
  }
}
