package com.larena.boxbreaker.runtime.user;

/**
 * Special authorities, modelled on IBM&nbsp;i ({@code *ALLOBJ}, {@code *SECADM}, …).
 * A user profile holds a set of these; they become Spring Security authorities,
 * so endpoints can require them (e.g. user administration needs {@code SECADM}).
 */
public enum SpecialAuthority {
    ALLOBJ,     // all-object: broad access
    SECADM,     // security administration (manage user profiles)
    JOBCTL,     // job control
    SPLCTL,     // spool control
    SAVSYS,     // save/restore
    SERVICE,    // service tools
    AUDIT,      // auditing
    IOSYSCFG    // I/O system configuration
}
