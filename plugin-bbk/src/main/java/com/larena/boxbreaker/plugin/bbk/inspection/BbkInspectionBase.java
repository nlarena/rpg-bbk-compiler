package com.larena.boxbreaker.plugin.bbk.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.larena.boxbreaker.plugin.bbk.types.BbkType;
import com.larena.boxbreaker.plugin.bbk.types.BbkUnknownType;
import org.jetbrains.annotations.NotNull;

/**
 * Common base for every BBK type-aware inspection.
 *
 * <ul>
 *   <li>Groups all BBK inspections under the {@code "BBK"} node in
 *       Settings → Inspections.</li>
 *   <li>Provides {@link #shouldSkip(BbkType...)} so concrete inspections can
 *       silently bail when any operand is {@link BbkUnknownType} — avoids false
 *       positives on WIP / unparseable code.</li>
 * </ul>
 */
public abstract class BbkInspectionBase extends LocalInspectionTool {

    @Override
    public @NotNull String getGroupDisplayName() {
        return "BBK";
    }

    /**
     * {@code true} if any of the supplied types is {@link BbkUnknownType}.
     * When the inferrer cannot resolve a type, we must not report a "mismatch"
     * — the user is still typing or the PSI is broken. Returning {@code true}
     * here tells the caller to skip the report.
     */
    protected static boolean shouldSkip(@NotNull BbkType... types) {
        for (BbkType t : types) {
            if (t instanceof BbkUnknownType) return true;
        }
        return false;
    }
}
