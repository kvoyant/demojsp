package com.example.demojsp.common.banner.web;

import com.example.demojsp.common.banner.store.BannerImageStore;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * [CF-BC-20 / 공통#45] ADMIN 이 업로드한 배너 이미지 조회 (고객 화면 &lt;img src&gt;).
 *
 * <pre>
 *  GET /common/banner/images/{32자리 난수}.{png|jpg|gif|webp}
 * </pre>
 * 파일명은 난수라 바뀌지 않으므로 오래 캐시한다. nosniff 로 브라우저가 다른 형식으로 해석하지 못하게 한다.
 */
@RestController
public class BannerImageController {

    private final BannerImageStore imageStore;

    public BannerImageController(BannerImageStore imageStore) {
        this.imageStore = imageStore;
    }

    @GetMapping("/common/banner/images/{name:[a-f0-9]{32}\\.(?:png|jpg|gif|webp)}")
    public ResponseEntity<byte[]> image(@PathVariable("name") String name) {
        byte[] data = imageStore.load(name);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(BannerImageStore.contentType(name)))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(data);
    }
}
