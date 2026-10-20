package net.okjsp

import org.springframework.web.multipart.MultipartFile
import java.io.File

class FileController {

    def fileService

    def image() {

        MultipartFile imageFile = request.getFile("files")

        if(!imageFile.empty) {
            def stored = fileService.saveUpload(imageFile, "images")

            render "<script>parent.\$.imageUploaded('${grailsApplication.config.grails.fileURL}/images/${stored}', '${stored}');</script>"
        }
    }

    def imageChecked() {

        MultipartFile imageFile = request.getFile("files")

        if(!imageFile.empty) {
            def stored = fileService.saveImage(imageFile, "images")

            if(stored) {
                render "<script>parent.\$.imageUploaded('${grailsApplication.config.grails.fileURL}/images/${stored}', '${stored}');</script>"
            } else {
                render status: 400, text: "<script>parent.\$.imageUploadFailed();</script>"
            }
        }
    }
}
