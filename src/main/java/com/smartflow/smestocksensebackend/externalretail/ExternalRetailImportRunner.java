package com.smartflow.smestocksensebackend.externalretail;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;

@Component
public class ExternalRetailImportRunner implements ApplicationRunner {

    private final ExternalRetailImporter importer;
    private final ConfigurableApplicationContext context;

    public ExternalRetailImportRunner(ExternalRetailImporter importer, ConfigurableApplicationContext context) {
        this.importer = importer;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("external-retail-import")) {
            return;
        }
        Path source = Path.of(value(args, "source", "../data_set"));
        Path mapping = Path.of(value(args, "mapping", "src/main/resources/ai/external-retail-mapping-v1.csv"));
        LocalDate start = LocalDate.parse(value(args, "start-date", ExternalRetailImporter.DEFAULT_START.toString()));
        LocalDate end = LocalDate.parse(value(args, "end-date", ExternalRetailImporter.DEFAULT_END.toString()));
        boolean dryRun = Boolean.parseBoolean(value(args, "dry-run", "true"));
        int batchSize = Integer.parseInt(value(args, "batch-size", "1000"));
        ExternalRetailImporter.ImportResult result = importer.run(
                new ExternalRetailImporter.ImportOptions(source, mapping, start, end, dryRun, batchSize));
        System.out.println(result.toReport());
        context.close();
    }

    private String value(ApplicationArguments args, String name, String defaultValue) {
        return args.containsOption(name) && !args.getOptionValues(name).isEmpty()
                ? args.getOptionValues(name).get(0)
                : defaultValue;
    }
}
