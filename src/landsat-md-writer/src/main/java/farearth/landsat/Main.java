/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.landsat;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author eduan
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Parameters params = parse(args);
        if (params != null) {
            FrameSyncBuilder builder = new FrameSyncBuilder();
            params.getRealtimeOutput().ifPresent(dir -> builder.writeRealtimeData(dir, params.isQueued()));
            params.getPlaybackOutput().ifPresent(dir -> builder.writePlaybackData(dir, params.isQueued()));
            params.getSsohOutput().ifPresent(dir -> builder.writeStateOfHealthData(dir, params.isQueued()));
            if (params.isDecodeLdpc()) {
                builder.processLdpc(params.isDeRandomize(), params.isDetectErrors(), params.getLdpsMaxErrorsToFix().orElse(60));
            }

            try (FrameSynchronizer frameSynchronizer = builder.build()) {
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                try (FileChannel channel = FileChannel.open(params.getInput().toPath(), StandardOpenOption.READ)) {
                    while (channel.read(buffer) >= 0) {
                        buffer.flip();
                        frameSynchronizer.process(buffer);
                        buffer.clear();
                    }
                }
            }
        } else {
            System.exit(1);
        }

    }

    static interface Parameters {

        File getInput();

        boolean isQueued();

        Optional<File> getRealtimeOutput();

        Optional<File> getPlaybackOutput();

        Optional<File> getSsohOutput();

        boolean isDecodeLdpc();

        boolean isDeRandomize();

        boolean isDetectErrors();

        Optional<Integer> getLdpsMaxErrorsToFix();
    }

    static Parameters parse(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = getOptions();

        try {
            CommandLine cmd = parser.parse(options, args);

            boolean queued = cmd.hasOption("q");
            File input = new File(cmd.getOptionValue("input-file"));
            if (!input.isFile()) {
                throw new IllegalArgumentException(String.format("The input file '%s' is invalid", input.getAbsolutePath()));
            }

            Optional<File> realtimeOutput = Optional.ofNullable(cmd.getOptionValue("realtime-output")).map(File::new);
            if (realtimeOutput.isPresent() && !realtimeOutput.get().isDirectory()) {
                throw new IllegalArgumentException(String.format("The realtime output directory '%s' is invalid.", realtimeOutput.get().getAbsolutePath()));
            }

            Optional<File> playbackOutput = Optional.ofNullable(cmd.getOptionValue("playback-output")).map(File::new);
            if (playbackOutput.isPresent() && !playbackOutput.get().isDirectory()) {
                throw new IllegalArgumentException(String.format("The playback output directory '%s' is invalid.", playbackOutput.get().getAbsolutePath()));
            }

            Optional<File> ssohOutput = Optional.ofNullable(cmd.getOptionValue("ssoh-output")).map(File::new);
            if (ssohOutput.isPresent() && !ssohOutput.get().isDirectory()) {
                throw new IllegalArgumentException(String.format("The SSOH output directory '%s' is invalid.", ssohOutput.get().getAbsolutePath()));
            }

            if (!realtimeOutput.isPresent() && !playbackOutput.isPresent() && !ssohOutput.isPresent()) {
                throw new IllegalArgumentException("No outputs specified.");
            }

            boolean decodeLdpc = cmd.hasOption("l");
            boolean derandomizeLdpcFrame = cmd.hasOption("d");
            boolean detectErrors = cmd.hasOption("c");
            Optional<Integer> maxLdpcErrors = Optional.ofNullable(cmd.getOptionValue("max-ldpc-errors")).map(Integer::valueOf);

            return new Parameters() {
                @Override
                public File getInput() {
                    return input;
                }

                @Override
                public boolean isQueued() {
                    return queued;
                }

                @Override
                public Optional<File> getRealtimeOutput() {
                    return realtimeOutput;
                }

                @Override
                public Optional<File> getPlaybackOutput() {
                    return playbackOutput;
                }

                @Override
                public Optional<File> getSsohOutput() {
                    return ssohOutput;
                }

                @Override
                public boolean isDecodeLdpc() {
                    return decodeLdpc;
                }

                @Override
                public boolean isDeRandomize() {
                    return derandomizeLdpcFrame;
                }

                @Override
                public boolean isDetectErrors() {
                    return detectErrors;
                }

                @Override
                public Optional<Integer> getLdpsMaxErrorsToFix() {
                    return maxLdpcErrors;
                }

            };
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            System.out.println(e.getMessage());
            formatter.printHelp("landsat8-md-writer", options);
            return null;
        }
    }

    private static Options getOptions() {
        return getBuilder()
                .add("i", "input-file", true, "Required. Input file", true)
                .add("q", "queued", false, "Optional. Whether to queue output writing.", false)
                .add("r", "realtime-output", true, "Optional. Realtime output data directory", false)
                .add("p", "playback-output", true, "Optional. Playback output data directory", false)
                .add("s", "ssoh-output", true, "Optional. Satellite-state-of-health (SSOH) output data directory", false)
                .add("l", "ldpc-enabled", false, "Optional. Enable LDPC decoding", false)
                .add("d", "derandomize-ldpc-frame", false, "Optional. Enable LDPC frame derandomization", false)
                .add("c", "ldpc-correct-errors", false, "Optional. Correct LDPC errors", false)
                .add("m", "max-ldpc-errors", true, "Optional. Max LDPC errors to fix (defaults to 60)", false)
                .build();
    }

    private static OptionsBuilder getBuilder() {
        Options options = new Options();

        return new OptionsBuilder() {

            @Override
            public OptionsBuilder add(String opt, String longOpt, boolean hasArg, String description, boolean required) {
                Option input = new Option(opt, longOpt, hasArg, description);
                input.setRequired(required);
                options.addOption(input);
                return this;
            }

            @Override
            public Options build() {
                return options;
            }
        };
    }

    private static interface OptionsBuilder {

        OptionsBuilder add(String opt, String longOpt, boolean hasArg, String description, boolean required);

        Options build();
    }

}
