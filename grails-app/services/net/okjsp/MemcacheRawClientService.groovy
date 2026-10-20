package net.okjsp

import java.security.MessageDigest

/**
 * Minimal text-protocol memcached adapter used for the page fragment cache.
 * Talks to the cache server directly over a socket, so it has no extra library
 * dependency. Host and port come from okky.memcached.host / okky.memcached.port.
 *
 * The plain methods (get, set, delete, incr, getMulti) take the key exactly as given.
 * The *Checked methods derive a namespaced, hashed key first.
 */
class MemcacheRawClientService {

    static transactional = false

    def grailsApplication

    private static final int MAX_KEY_LENGTH = 250
    private static final int SOCKET_TIMEOUT_MS = 1500

    String host() {
        grailsApplication?.config?.okky?.memcached?.host ?: 'localhost'
    }

    int port() {
        (grailsApplication?.config?.okky?.memcached?.port ?: 11211) as int
    }

    /** Runs one exchange: the writer puts the command on the wire, then the first reply is read. */
    private String exchange(Closure writer) {
        Socket socket = new Socket()
        try {
            socket.connect(new InetSocketAddress(host(), port()), SOCKET_TIMEOUT_MS)
            socket.setSoTimeout(SOCKET_TIMEOUT_MS)
            OutputStream out = socket.getOutputStream()
            writer.call(out)
            out.flush()
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), 'UTF-8'))
            String first = reader.readLine()
            if (first == null || !first.startsWith('VALUE')) {
                return first
            }
            StringBuilder reply = new StringBuilder(first).append('\n')
            String line
            while ((line = reader.readLine()) != null && line != 'END') {
                reply.append(line).append('\n')
            }
            return reply.toString()
        } catch (IOException e) {
            log.warn("memcached unavailable at ${host()}:${port()}: ${e.message}")
            return null
        } finally {
            socket.close()
        }
    }

    /** Namespaced SHA-256 key for user supplied names; rejects names that cannot be a sensible cache key. */
    String safeKey(String namespace, String raw) {
        if (raw == null || raw.isEmpty() || raw.length() > MAX_KEY_LENGTH || raw =~ /[\x00-\x20\x7f]/) {
            throw new IllegalArgumentException('invalid cache key')
        }
        byte[] digest = MessageDigest.getInstance('SHA-256').digest(raw.getBytes('UTF-8'))
        namespace + ':' + digest.collect { String.format('%02x', it & 0xff) }.join('')
    }

    String get(String key) {
        exchange { OutputStream out ->
            out.write("get ${key}\r\n".getBytes('UTF-8')) // SINK: PLANTED-GR-HR-396
        }
    }

    String getChecked(String namespace, String raw) {
        String key = safeKey(namespace, raw)
        exchange { OutputStream out ->
            out.write("get ${key}\r\n".getBytes('UTF-8')) // SAFE_SINK: PLANTED-GR-HR-396-safe
        }
    }

    String set(String key, int ttl, String value) {
        exchange { OutputStream out ->
            out.write("set ${key} 0 ${ttl} ${value.length()}\r\n${value}\r\n".getBytes('UTF-8')) // SINK: PLANTED-GR-HR-397
        }
    }

    String setChecked(String namespace, String raw, int ttl, String value) {
        String key = safeKey(namespace, raw)
        byte[] body = value.getBytes('UTF-8')
        exchange { OutputStream out ->
            out.write("set ${key} 0 ${ttl} ${body.length}\r\n".getBytes('UTF-8')) // SAFE_SINK: PLANTED-GR-HR-397-safe
            out.write(body)
            out.write('\r\n'.getBytes('UTF-8'))
        }
    }

    String delete(String key) {
        exchange { OutputStream out ->
            out.write("delete ${key}\r\n".getBytes('UTF-8')) // SINK: PLANTED-GR-HR-398
        }
    }

    String deleteChecked(String namespace, String raw) {
        String key = safeKey(namespace, raw)
        exchange { OutputStream out ->
            out.write("delete ${key}\r\n".getBytes('UTF-8')) // SAFE_SINK: PLANTED-GR-HR-398-safe
        }
    }

    String incr(String key) {
        exchange { OutputStream out ->
            out.write("incr ${key} 1\r\n".getBytes('UTF-8')) // SINK: PLANTED-GR-HR-399
        }
    }

    String incrChecked(String namespace, String raw) {
        String key = safeKey(namespace, raw)
        exchange { OutputStream out ->
            out.write("incr ${key} 1\r\n".getBytes('UTF-8')) // SAFE_SINK: PLANTED-GR-HR-399-safe
        }
    }

    String getMulti(List<String> keys) {
        String joined = keys.join(' ')
        exchange { OutputStream out ->
            out.write("get ${joined}\r\n".getBytes('UTF-8')) // SINK: PLANTED-GR-HR-400
        }
    }

    String getMultiChecked(String namespace, List<String> raws) {
        String joined = raws.collect { safeKey(namespace, it) }.join(' ')
        exchange { OutputStream out ->
            out.write("get ${joined}\r\n".getBytes('UTF-8')) // SAFE_SINK: PLANTED-GR-HR-400-safe
        }
    }
}
