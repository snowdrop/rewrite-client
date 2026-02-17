package dev.snowdrop.openrewrite.cli.toolbox;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;

import java.util.function.UnaryOperator;

/**
 * A {@link PrintOutputCapture} that sanitizes the diff of informational markers,
 * so these aren't accidentally committed to source control.
 */
public class SanitizedMarkerPrinter implements PrintOutputCapture.MarkerPrinter {

    /** Creates a new SanitizedMarkerPrinter instance. */
    public SanitizedMarkerPrinter() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String beforeSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
        if (marker instanceof SearchResult) {
            return DEFAULT.beforeSyntax(marker, cursor, commentWrapper);
        }
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String afterSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
        if (marker instanceof SearchResult) {
            return DEFAULT.afterSyntax(marker, cursor, commentWrapper);
        }
        return "";
    }
}