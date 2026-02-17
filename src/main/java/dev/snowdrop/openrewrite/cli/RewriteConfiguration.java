/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.snowdrop.openrewrite.cli;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Configuration properties for the Rewrite CLI application.
 * These properties can be set via environment variables or application.properties.
 * <p>
 * Environment variable examples:
 * - REWRITE_CONFIG_LOCATION=/path/to/rewrite.yml
 * - REWRITE_SIZE_THRESHOLD_MB=20
 * - REWRITE_EXPORT_DATATABLES=true
 */
@ConfigMapping(prefix = "rewrite")
public interface RewriteConfiguration {

    /**
     * Location of the rewrite.yml configuration file.
     * Can be overridden with REWRITE_CONFIG_LOCATION environment variable.
     *
     * @return the configuration file location
     */
    @WithDefault("rewrite.yml")
    String configLocation();

    /**
     * Boolean to execute the recipe in dry run mode.
     * Can be overridden with REWRITE_DRY_RUN environment variable.
     *
     * @return true if dry run mode is enabled
     */
    @WithDefault("true")
    boolean dryRun();

    /**
     * Size threshold in MB for large files.
     * Can be overridden with REWRITE_SIZE_THRESHOLD_MB environment variable.
     *
     * @return the size threshold in megabytes
     */
    @WithDefault("10")
    int sizeThresholdMb();

    /**
     * Whether to export datatables to CSV files.
     * Can be overridden with REWRITE_EXPORT_DATATABLES environment variable.
     *
     * @return true if datatables should be exported
     */
    @WithDefault("true")
    boolean exportDatatables();

    /**
     * Whether to fail on invalid active recipes.
     * Can be overridden with REWRITE_FAIL_ON_INVALID_ACTIVE_RECIPES environment variable.
     *
     * @return true if the build should fail on invalid recipes
     */
    @WithDefault("false")
    boolean failOnInvalidActiveRecipes();

    /**
     * Comma-separated list of plain text file masks.
     * Can be overridden with REWRITE_PLAIN_TEXT_MASKS environment variable.
     *
     * @return the plain text masks, or empty if not configured
     */
    Optional<String> plainTextMasks();

    /**
     * Comma-separated list of file patterns to exclude.
     * Can be overridden with REWRITE_EXCLUSIONS environment variable.
     *
     * @return the exclusion patterns, or empty if not configured
     */
    Optional<String> exclusions();
}