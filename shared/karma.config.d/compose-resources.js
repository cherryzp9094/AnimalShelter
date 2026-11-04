// Compose 리소스(문구·글꼴·아이콘)를 Karma가 내도록 한다 — UI 흐름 테스트(브리핑 C6)가 App()을 그리면
// 웹 리소스 로더가 페이지 기준 ./composeResources/… 를 받으러 간다. 테스트 번들에는 main 리소스가 없어서
// processedResources의 main 폴더를 그대로 내고 그 경로로 프록시한다.
const path = require("path");

// basePath = <루트>/build/wasm/packages/<모듈>-test
const root = path.resolve(config.basePath, "../../../..");
const resources = path.resolve(root, "shared/build/processedResources/wasmJs/main/composeResources");

config.files.push({
    pattern: path.join(resources, "**/*"),
    included: false,
    served: true,
    watched: false,
    nocache: true,
});
config.proxies["/composeResources/"] = "/absolute" + resources + "/";

// UI 흐름 테스트는 화면 전환·스낵바를 실제 시간으로 기다린다 — mocha 기본 2초 제한을 늘린다.
config.client = config.client || {};
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 60000 });
