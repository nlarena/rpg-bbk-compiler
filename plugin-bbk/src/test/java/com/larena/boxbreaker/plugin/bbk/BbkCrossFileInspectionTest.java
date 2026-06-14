package com.larena.boxbreaker.plugin.bbk;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.larena.boxbreaker.plugin.bbk.inspection.*;

import java.util.List;

/**
 * Reproduces the user-reported red squiggly using the EXACT example files
 * from tests/boxbreaker/examples/crossfile, to identify what is flagged.
 */
public class BbkCrossFileInspectionTest extends BasePlatformTestCase {

    public void testCrossFileMemberAccessNotFlaggedAsUnresolved() {
        myFixture.addFileToProject("common-types.bbk",
            "DCL-DS customer QUALIFIED {\n" +
            "  id        INT(10);\n" +
            "  firstName CHAR(50);\n" +
            "  lastName  CHAR(50);\n" +
            "  email     VARCHAR(100);\n" +
            "  birthDate DATE;\n" +
            "  isActive  BOOL;\n" +
            "}\n" +
            "DCL-DS order QUALIFIED {\n" +
            "  orderId    INT(10);\n" +
            "  customerId INT(10);\n" +
            "  total      PACKED(11:2);\n" +
            "  orderDate  DATE;\n" +
            "  status     CHAR(20);\n" +
            "}\n" +
            "DCL-DS addressTemplate TEMPLATE QUALIFIED {\n" +
            "  street  CHAR(100);\n" +
            "  city    CHAR(50);\n" +
            "  zip     CHAR(10);\n" +
            "  country CHAR(50);\n" +
            "}\n");

        myFixture.addFileToProject("common-constants.bbk",
            "DCL-C MAX_RETRIES   5;\n" +
            "DCL-C COMPANY_NAME  \"Acme Corporation\";\n" +
            "DCL-C PI            3.14159d;\n" +
            "DCL-C IS_DEBUG      true;\n" +
            "DCL-C DEFAULT_PORT0  8080;\n");

        myFixture.addFileToProject("common-procs.bbk",
            "DCL-PR sendNotification(custId INT(10) VALUE, msg VARCHAR(500) CONST);\n" +
            "DCL-PR computeTax(amount PACKED(11:2) VALUE) -> PACKED(11:2);\n" +
            "DCL-PR formatCurrency(amount PACKED(11:2) VALUE) -> VARCHAR(50);\n" +
            "DCL-PROC validateCustomer(cust LIKEDS(customer) CONST) -> BOOL EXPORT {\n" +
            "  return cust.isActive;\n" +
            "}\n" +
            "DCL-PROC processOrder(ord LIKEDS(order) CONST) -> BOOL EXPORT {\n" +
            "  return true;\n" +
            "}\n" +
            "DCL-PROC greetCustomer(cust LIKEDS(customer) CONST) {\n" +
            "  return;\n" +
            "}\n");

        myFixture.configureByText("main.bbk",
            "CTL-OPT MAIN(runDemo);\n" +
            "DCL-PR localHelper(n INT(10) VALUE) -> INT(10);\n" +
            "DCL-PROC runDemo EXPORT {\n" +
            "  DCL-S customerOrder LIKEDS(customer);\n" +
            "  DCL-S currentOrder  LIKEDS(order);\n" +
            "  DCL-S retriesLeft   INT(10) INZ(MAX_RETRIES);\n" +
            "  DCL-S i      INT(10);\n" +
            "  DCL-S valid  BOOL;\n" +
            "  currentOrder.orderId = 1;\n" +
            "  valid = validateCustomer(customerOrder);\n" +
            "  if (valid) {\n" +
            "    greetCustomer(customerOrder);\n" +
            "    if (processOrder(currentOrder)) {\n" +
            "      print(\"Order \" + char(currentOrder.orderId) + \" ok\");\n" +
            "    }\n" +
            "  }\n" +
            "  for (i = 1; i <= MAX_RETRIES; i += 1) {\n" +
            "    if (i == DEFAULT_PORT0) {\n" +
            "      break;\n" +
            "    }\n" +
            "  }\n" +
            "  retriesLeft = localHelper(retriesLeft);\n" +
            "}\n" +
            "DCL-PROC localHelper(n INT(10) VALUE) -> INT(10) {\n" +
            "  return n - 1;\n" +
            "}\n");

        myFixture.enableInspections(
            new BbkAssignmentTypeMismatchInspection(),
            new BbkReturnTypeMismatchInspection(),
            new BbkCallArgumentTypeMismatchInspection(),
            new BbkCallArgumentCountMismatchInspection(),
            new BbkConditionNotBoolInspection(),
            new BbkInzValueTypeMismatchInspection(),
            new BbkUnresolvedReferenceInspection());

        List<HighlightInfo> infos = myFixture.doHighlighting();
        String src = myFixture.getFile().getText();
        StringBuilder problems = new StringBuilder();
        for (HighlightInfo info : infos) {
            if (info.getDescription() == null) continue;
            String text = src.substring(info.getStartOffset(), info.getEndOffset()).replace("\n", "\\n");
            problems.append("\n  [").append(info.getSeverity()).append("] '")
                .append(text).append("' -> ").append(info.getDescription());
        }
        // Cross-file member access (currentOrder.orderId) must NOT raise an
        // "unresolved reference" — the DS `order` lives in common-types.bbk.
        assertEquals("Expected no inspection problems on the cross-file example, but got:"
            + problems, 0, problems.length());
    }
}
