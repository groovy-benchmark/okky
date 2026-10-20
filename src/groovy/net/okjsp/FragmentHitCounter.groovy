package net.okjsp

/** Counts how often a cached page fragment is requested; the strategy decides how the counter is keyed. */
interface FragmentHitCounter {
    String hit(MemcacheRawClientService memcache, String page)
}

/** Counter keyed on the page name exactly as it arrives in the request. */
class RawPageHitCounter implements FragmentHitCounter {
    String hit(MemcacheRawClientService memcache, String page) {
        memcache.incr('hits:' + page)
    }
}

/** Counter kept under a fixed namespace with a validated and hashed page name. */
class ValidatedPageHitCounter implements FragmentHitCounter {
    String hit(MemcacheRawClientService memcache, String page) {
        memcache.incrChecked('hits', page)
    }
}
