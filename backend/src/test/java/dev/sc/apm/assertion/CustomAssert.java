package dev.sc.apm.assertion;

import dev.sc.apm.entity.Client;
import dev.sc.apm.entity.CreditAgreement;
import dev.sc.apm.entity.CreditApplication;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomAssert {
    public static void assertEqualsClient(Client c1, Client c2) {
        if (c1 == null && c2 == null) {
            return;
        } else if (c1 == null) {
            fail();
        } else if (c2 == null) {
            fail();
        }

        assertEquals(c1.getId(), c2.getId());
        assertEquals(c1.getFirstName(), c2.getFirstName());
        assertEquals(c1.getLastName(), c2.getLastName());
        assertEquals(c1.getMiddleName(), c2.getMiddleName());
        assertEquals(c1.getMaritalStatus(), c2.getMaritalStatus());
        assertEquals(c1.getPassport(), c2.getPassport());
        assertEquals(c1.getPhone(), c2.getPhone());
        assertEquals(c1.getAddress(), c2.getAddress());
        assertEquals(c1.getOrganizationName(), c2.getOrganizationName());
        assertEquals(c1.getPosition(), c2.getPosition());
        assertEquals(c1.getEmploymentPeriod(), c2.getEmploymentPeriod());

        var apps1 = c1.getCreditApplications();
        var apps2 = c2.getCreditApplications();

        assertEquals(apps1.size(), apps2.size());

        for (int i = 0; i < apps1.size(); i++) {
            assertEquals(apps1.get(i).getId(), apps2.get(i).getId());
        }
    }

    public static void assertEqualsCreditApplication(CreditApplication c1, CreditApplication c2) {

        if (c1 == null && c2 == null) {
            return;
        } else if (c1 == null) {
            fail();
        } else if (c2 == null) {
            fail();
        }

        assertEquals(c1.getId(), c2.getId());
        assertEquals(c1.getClient().getId(), c2.getClient().getId());
        assertEquals(
                c1.getRequestedAmount().subtract(c2.getRequestedAmount()).abs().toBigInteger(),
                BigInteger.ZERO

        );
        assertEquals(c1.getStatus(), c2.getStatus());

        if (c1.getApprovedAmount() == null && c2.getApprovedAmount() == null) {

        } else if (c1.getApprovedAmount() == null) {
            fail();
        } else if (c2.getApprovedAmount() == null) {
            fail();
        } else {
            assertEquals(
                    c1.getApprovedAmount().subtract(c2.getApprovedAmount()).abs().toBigInteger(),
                    BigInteger.ZERO

            );
        }

        if (c1.getApprovedTerm() == null && c2.getApprovedTerm() == null) {

        } else if (c1.getApprovedTerm() == null) {
            fail();
        } else if (c2.getApprovedTerm() == null) {
            fail();
        } else {
            assertEquals(c1.getApprovedTerm(), c2.getApprovedTerm());
        }

        assertEqualsLocalDateTime(c1.getCreatedAt(), c2.getCreatedAt());

        if (c1.getCreditAgreement() == null && c2.getCreditAgreement() == null) {
            return;
        } else if (c1.getCreditAgreement() == null) {
            fail();
        } else if (c2.getCreditAgreement() == null) {
            fail();
        }

        assertEquals(c1.getCreditAgreement().getId(), c2.getCreditAgreement().getId());
    }

    public static void assertEqualsCreditAgreement(CreditAgreement c1, CreditAgreement c2) {

        if (c1 == null && c2 == null) {
            return;
        } else if (c1 == null) {
            fail();
        } else if (c2 == null) {
            fail();
        }

        assertEquals(c1.getId(), c2.getId());
        assertEqualsLocalDateTime(c1.getSignedAt(), c2.getSignedAt());
        assertEquals(c1.getSigningStatus(), c2.getSigningStatus());
        assertEquals(c1.getApplication().getId(), c2.getApplication().getId());
    }

    public static void assertEqualsLocalDateTime(LocalDateTime expected, LocalDateTime actual) {
        if (expected != null && actual != null) {
            assertTrue(Duration.between(expected, actual).getSeconds() < 1);
        } else if (expected == null && actual == null) {
            assertEquals(expected, actual);
        } else {
            fail();
        }
    }
}
