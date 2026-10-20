package net.okjsp

import grails.transaction.Transactional

import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.net.ssl.SSLContext

@Transactional
class UserService {

    EncryptService encryptService
    def grailsApplication

    /**
     * Save User with relationship
     * @param userInstance
     * @return
     */
    def saveUser(User userInstance) {

        userInstance.avatar.setPictureBySns(userInstance)

        // 유저 연관 정보 선 저장
        userInstance.person.save(failOnError: true)
        userInstance.avatar.save(failOnError: true)

        def result = userInstance.save(failOnError: true)

        // 유저 권한 생성
        UserRole.create(userInstance, Role.findByAuthority('ROLE_USER'), true)

        result
    }
    
    def updateUser(User userInstance) {

        userInstance.avatar.setPictureBySns(userInstance)

        // 유저 연관 정보 선 저장
        userInstance.person.save(failOnError: true)
        userInstance.avatar.save(failOnError: true)

        def result = userInstance.save(failOnError: true)

        result
    }

    def createConfirmEmail(User userInstance) {

        def now = new Date()

        ConfirmEmail.where { user == userInstance }.deleteAll()

        def secured = "${now.time}_${userInstance.username}_${userInstance.person.email}_${grailsApplication.config.grails.mail.key}".encodeAsSHA256().encodeAsBase64()

        new ConfirmEmail(user: userInstance, securedKey: secured, email: userInstance.person.email, dateExpired: new Date(now.time+(30*60*1000))).save(flush: true, failOnError: true)

        return secured

    }
    
    def getManaedAvatars(def user = null) {

        def managedAvatar = ManagedUser.findAll()*.user*.avatar

        if(user) {
            managedAvatar.remove(user.avatar)
        }

        managedAvatar
    }
    
    def getRealIp(def request) {
        String ipAddress = request.getHeader("X-Fowarded-For")
        
        if (!ipAddress)
            ipAddress = request.getHeader("X-Real-Ip")
        
        if (!ipAddress)
            ipAddress = request.getRemoteAddr()

        ipAddress
    }
    
    def getDmUserCvsString() {
        
        def persons = Person.findAllByDmAllowed(true)
        
        StringBuilder sb = new StringBuilder()
        
        String url = String.format("%s/user/rejectDM?k=", grailsApplication.config.grails.serverURL)
        
        for(Person p : persons) {
            
            String name = p.getFullName()
            String email = p.getEmail()

            if(email && !email.empty) {
                String enc = new String(encryptService.encrypt(email.getBytes()))
                sb.append(String.format("%s,%s,%s%s%s", name, email, url, enc, System.lineSeparator()))
            }
        }
        
        sb.toString()
    }

    private static final Map<String, TlsProfile> PARTNER_TLS = [legacy: new LegacyPartnerTls(), current: new CurrentPartnerTls()]

    private SecretKey rejectLinkSecret = null
    private SecretKey sessionLinkSecret = null

    private SecretKey getRejectLinkSecret() {
        if(rejectLinkSecret == null) {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256")
            keyGen.init(64) // SINK: PLANTED-GR-HR-106
            rejectLinkSecret = keyGen.generateKey()
        }
        rejectLinkSecret
    }

    private SecretKey getSessionLinkSecret() {
        if(sessionLinkSecret == null) {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256")
            keyGen.init(256) // SAFE_SINK: PLANTED-GR-HR-106-safe
            sessionLinkSecret = keyGen.generateKey()
        }
        sessionLinkSecret
    }

    /**
     * Signature appended to the DM reject link so the address cannot be swapped
     */
    def signRejectLink(String email) {
        Mac mac = Mac.getInstance("HmacSHA256")
        mac.init(getRejectLinkSecret())
        mac.doFinal(email.getBytes("UTF-8")).encodeHex().toString()
    }

    def signSessionLink(String sessionId) {
        Mac mac = Mac.getInstance("HmacSHA256")
        mac.init(getSessionLinkSecret())
        mac.doFinal(sessionId.getBytes("UTF-8")).encodeHex().toString()
    }

    /**
     * TLS context for calls to the partner OTP gateway, chosen by profile name
     */
    SSLContext partnerSslContext(String profile) {
        TlsProfile tls = PARTNER_TLS[profile] ?: PARTNER_TLS.legacy
        tls.create()
    }

    def withdraw(User user) {

        // 게시글에 대한 익명 처리
        Article.executeUpdate("update Article set anonymity = true, aNickName = :nickname where author = :author",
                [nickname : user.avatar.nickname, author : user.avatar])
        // 댓글에 대한 익명 처리
        Content.executeUpdate("update Content set anonymity = true, aNickName = :nickname where author = :author",
                [nickname : user.avatar.nickname, author : user.avatar])

        // DM 발송에서 제외
        def person = user.person

        person.dmAllowed = false
        person.save(flush: true)

        user.withdraw = true
        user.dateWithdraw = new Date()
        user.accountLocked = true
        user.accountExpired = true
        user.enabled = false
        user.save(flush: true)
    }
}

interface TlsProfile {
    SSLContext create()
}

class LegacyPartnerTls implements TlsProfile {
    SSLContext create() {
        SSLContext ctx = SSLContext.getInstance("TLSv1") // SINK: PLANTED-GR-HR-109
        ctx.init(null, null, null)
        ctx
    }
}

class CurrentPartnerTls implements TlsProfile {
    SSLContext create() {
        SSLContext ctx = SSLContext.getInstance("TLSv1.2") // SAFE_SINK: PLANTED-GR-HR-109-safe
        ctx.init(null, null, null)
        ctx
    }
}
