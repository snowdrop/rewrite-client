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

public class ResultsContainer {
    private List<Result> generated = new ArrayList<>();
    private List<Result> deleted = new ArrayList<>();
    private List<Result> moved = new ArrayList<>();
    private List<Result> refactoredInPlace = new ArrayList<>();
    private Map<String, RecipeRun> recipeRuns;

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

    public boolean isNotEmpty() {
        return !generated.isEmpty() || !deleted.isEmpty() || !moved.isEmpty() || !refactoredInPlace.isEmpty();
    }

    public List<Result> getGenerated() {
        return generated;
    }

    public void setGenerated(List<Result> generated) {
        this.generated = generated;
    }

    public List<Result> getDeleted() {
        return deleted;
    }

    public void setDeleted(List<Result> deleted) {
        this.deleted = deleted;
    }

    public List<Result> getMoved() {
        return moved;
    }

    public void setMoved(List<Result> moved) {
        this.moved = moved;
    }

    public List<Result> getRefactoredInPlace() {
        return refactoredInPlace;
    }

    public void setRefactoredInPlace(List<Result> refactoredInPlace) {
        this.refactoredInPlace = refactoredInPlace;
    }

    public Map<String, RecipeRun> getRecipeRuns() {
        return recipeRuns;
    }

    public void setRecipeRuns(Map<String, RecipeRun> recipeRuns) {
        this.recipeRuns = recipeRuns;
    }
}
