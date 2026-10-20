package net.okjsp

class MainController {

    def mainService
    def userService
    def springSecurityService
    def randomService

    def grailsCacheAdminService

    def index() {
        
        def mainBanners = Banner.where {
            type == BannerType.MAIN && visible == true            
        }.list()

        def mainBanner = mainBanners ? randomService.draw(mainBanners) : null


//        def techArticles = mainService.getTechArticles().clone().sort { Math.random() }
//        if(techArticles?.size() > 2) techArticles = techArticles.subList(0, 2)

        return [
            isIndex: true,
            choiceArticles: mainService.getChoiceArticles(),
            weeklyArticles: mainService.getWeeklyArticles(),
            questionsArticles: mainService.getQnaArticles(),
            communityArticles: mainService.getCommunityArticles(),
            columnArticle: mainService.getColumnArticle(),
//            techArticle: mainService.getTechArticle(),
            techArticles: mainService.getTechArticles(),
            promoteArticles: mainService.getPromoteArticles(),
            mainBanner : mainBanner
        ]
    }
    
    def flush() {
        grailsCacheAdminService.clearAllCaches();
    }

    /* Page fragment cache (memcached) */

    def fragmentShow() {
        render(text: mainService.getFragment(params.name as String) ?: '', contentType: 'text/plain')
    }

    def fragmentShowChecked() {
        try {
            render(text: mainService.getFragmentChecked(params.name as String) ?: '', contentType: 'text/plain')
        } catch (IllegalArgumentException e) {
            render(status: 400, text: 'invalid fragment name')
        }
    }

    def fragmentStore() {
        render(text: mainService.storeFragment(params.name as String, params.html as String) ?: '', contentType: 'text/plain')
    }

    def fragmentStoreChecked() {
        try {
            render(text: mainService.storeFragmentChecked(params.name as String, params.html as String) ?: '', contentType: 'text/plain')
        } catch (IllegalArgumentException e) {
            render(status: 400, text: 'invalid fragment name')
        }
    }

    def fragmentPurge() {
        String tenant = request.getHeader('X-Tenant') ?: 'public'
        render(text: mainService.purgeFragment(tenant, params.page as String) ?: '', contentType: 'text/plain')
    }

    def fragmentPurgeChecked() {
        try {
            render(text: mainService.purgeFragmentChecked(params.page as String) ?: '', contentType: 'text/plain')
        } catch (IllegalArgumentException e) {
            render(status: 400, text: 'invalid fragment name')
        }
    }

    def fragmentHit() {
        try {
            render(text: mainService.recordFragmentHit(params.scope as String, params.page as String) ?: '', contentType: 'text/plain')
        } catch (IllegalArgumentException e) {
            render(status: 400, text: 'invalid fragment name')
        }
    }
}
