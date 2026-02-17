package dev.snowdrop.openrewrite.cli.model;

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Container holding the results of the OpenRewrite recipe executions.
 */
public class ResultsContainer {
    private List<Result> generated = new ArrayList<>();
    private List<Result> deleted = new ArrayList<>();
    private List<Result> moved = new ArrayList<>();
    private List<Result> refactoredInPlace = new ArrayList<>();
    private Map<String, RecipeRun> recipeRuns;

    /**
     * Creates a new container from a map of recipe runs, categorizing each result.
     *
     * @param runs the map of recipe name to recipe run results
     */
    public ResultsContainer(Map<String, RecipeRun> runs) {
        this.recipeRuns = runs;

        runs.forEach((k, v) -> {
            List<Result> results = v.getChangeset().getAllResults();
            for (Result result : results) {
                if (result.getBefore() == null && result.getAfter() == null) {
                    continue;
                }
                if (result.getBefore() == null && result.getAfter() != null) {
                    generated.add(result);
                } else if (result.getBefore() != null && result.getAfter() == null) {
                    deleted.add(result);
                } else if (result.getBefore() != null && result.getAfter() != null &&
                    !result.getBefore().getSourcePath().equals(result.getAfter().getSourcePath())) {
                    moved.add(result);
                } else {
                    if (!result.diff(Paths.get("")).isEmpty()) {
                        refactoredInPlace.add(result);
                    }
                }
            }
        });
    }

    /**
     * Returns the first exception found across all results, or null if none.
     *
     * @return the first exception, or null
     */
    public @Nullable RuntimeException getFirstException() {
        for (Result result : generated) {
            for (RuntimeException error : getRecipeErrors(result)) {
                return error;
            }
        }
        for (Result result : deleted) {
            for (RuntimeException error : getRecipeErrors(result)) {
                return error;
            }
        }
        for (Result result : moved) {
            for (RuntimeException error : getRecipeErrors(result)) {
                return error;
            }
        }
        for (Result result : refactoredInPlace) {
            for (RuntimeException error : getRecipeErrors(result)) {
                return error;
            }
        }
        return null;
    }

    private List<RuntimeException> getRecipeErrors(Result result) {
        List<RuntimeException> exceptions = new ArrayList<>();
        new TreeVisitor<Tree, Integer>() {
            @Override
            public Tree preVisit(Tree tree, Integer integer) {
                Markers markers = tree.getMarkers();
                markers.findFirst(Markup.Error.class).ifPresent(e -> {
                    Optional<SourceFile> sourceFile = Optional.ofNullable(getCursor().firstEnclosing(SourceFile.class));
                    String sourcePath = sourceFile.map(SourceFile::getSourcePath).map(Path::toString).orElse("<unknown>");
                    exceptions.add(new RuntimeException("Error while visiting " + sourcePath + ": " + e.getDetail()));
                });
                return tree;
            }
        }.visit(result.getAfter(), 0);
        return exceptions;
    }

    /**
     * Returns whether this container has any results.
     *
     * @return true if there are generated, deleted, moved, or refactored results
     */
    public boolean isNotEmpty() {
        return !generated.isEmpty() || !deleted.isEmpty() || !moved.isEmpty() || !refactoredInPlace.isEmpty();
    }

    /**
     * Returns the list of generated source files.
     *
     * @return the generated results
     */
    public List<Result> getGenerated() {
        return generated;
    }

    /**
     * Sets the list of generated source files.
     *
     * @param generated the generated results
     */
    public void setGenerated(List<Result> generated) {
        this.generated = generated;
    }

    /**
     * Returns the list of deleted source files.
     *
     * @return the deleted results
     */
    public List<Result> getDeleted() {
        return deleted;
    }

    /**
     * Sets the list of deleted source files.
     *
     * @param deleted the deleted results
     */
    public void setDeleted(List<Result> deleted) {
        this.deleted = deleted;
    }

    /**
     * Returns the list of moved source files.
     *
     * @return the moved results
     */
    public List<Result> getMoved() {
        return moved;
    }

    /**
     * Sets the list of moved source files.
     *
     * @param moved the moved results
     */
    public void setMoved(List<Result> moved) {
        this.moved = moved;
    }

    /**
     * Returns the list of source files refactored in place.
     *
     * @return the refactored results
     */
    public List<Result> getRefactoredInPlace() {
        return refactoredInPlace;
    }

    /**
     * Sets the list of source files refactored in place.
     *
     * @param refactoredInPlace the refactored results
     */
    public void setRefactoredInPlace(List<Result> refactoredInPlace) {
        this.refactoredInPlace = refactoredInPlace;
    }

    /**
     * Returns the map of recipe runs.
     *
     * @return the recipe runs keyed by recipe name
     */
    public Map<String, RecipeRun> getRecipeRuns() {
        return recipeRuns;
    }

    /**
     * Sets the map of recipe runs.
     *
     * @param recipeRuns the recipe runs keyed by recipe name
     */
    public void setRecipeRuns(Map<String, RecipeRun> recipeRuns) {
        this.recipeRuns = recipeRuns;
    }
}