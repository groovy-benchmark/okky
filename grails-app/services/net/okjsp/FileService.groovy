package net.okjsp

import grails.transaction.Transactional
import org.springframework.web.multipart.MultipartFile

@Transactional
class FileService {

    static final List<String> IMAGE_EXTENSIONS = ['png', 'jpg', 'jpeg', 'gif']

    def grailsApplication

    def serviceMethod() {

    }

    /**
     * Stores an uploaded file below the configured upload root and returns the stored file name.
     */
    String saveUpload(MultipartFile file, String folder) {
        def ext = file.originalFilename.substring(file.originalFilename.lastIndexOf('.'))
        def mil = System.currentTimeMillis()

        File directory = new File("${grailsApplication.config.grails.filePath}/${folder}/")
        directory.mkdirs()

        file.transferTo(new File(directory, "${mil}${ext}")) // SINK: PLANTED-GR-HR-191

        return "${mil}${ext}"
    }

    /**
     * Stores an uploaded image below the configured upload root; returns null when it is not an image.
     */
    String saveImage(MultipartFile file, String folder) {
        def name = file.originalFilename
        def ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase()

        if (!(ext in IMAGE_EXTENSIONS) || !file.contentType?.startsWith('image/')) {
            return null
        }

        def mil = System.currentTimeMillis()

        File directory = new File("${grailsApplication.config.grails.filePath}/${folder}/")
        directory.mkdirs()

        file.transferTo(new File(directory, "${mil}.${ext}")) // SAFE_SINK: PLANTED-GR-HR-191-safe

        return "${mil}.${ext}"
    }

    /**
     * Stores a banner image below the configured upload root; returns null when it is not an image name.
     */
    String saveBannerImage(MultipartFile file) {
        def name = file.originalFilename

        if (!(name ==~ /(?i).+\.(png|jpe?g|gif)/)) {
            return null
        }

        def ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase()
        def mil = System.currentTimeMillis()

        file.transferTo(new File("${grailsApplication.config.grails.filePath}/banner/", "${mil}.${ext}")) // SAFE_SINK: PLANTED-GR-HR-194-safe

        return "${mil}.${ext}"
    }

    /**
     * Stores a replacement banner image below the configured upload root; returns null when it is not an image name.
     */
    String replaceBannerImage(MultipartFile file) {
        def name = file.originalFilename
        def ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase()

        if (!(ext in IMAGE_EXTENSIONS)) {
            return null
        }

        def mil = System.currentTimeMillis()

        file.transferTo(new File("${grailsApplication.config.grails.filePath}/banner/", "${mil}.${ext}")) // SAFE_SINK: PLANTED-GR-HR-195-safe

        return "${mil}.${ext}"
    }
}
