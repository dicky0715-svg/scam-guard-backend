package com.scamguard.api.controller;

import com.scamguard.api.dto.ChatMessage;
import com.scamguard.api.dto.ChatRequest;
import com.scamguard.api.dto.ChatResponse;
import com.scamguard.api.entity.QueryRecord;
import com.scamguard.api.repository.QueryRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")  // ← 改成咗，允許所有來源
public class ScamController {

    @Autowired
    private WebClient openAiWebClient;

    @Autowired
    private QueryRecordRepository queryRecordRepository;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/analyze")
    public Mono<Map<String, Object>> analyze(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");

        // 1. 設計 prompt（含ADCC + CyberDefender + 最新釣魚短訊樣本）
        String systemPrompt = """
            你係一位專業嘅香港反詐騙分析師。請根據以下香港警方ADCC、CyberDefender及最新釣魚短訊樣本資料庫分析用戶訊息。

            ===== ADCC最新騙案手法分類 (2025-2026) =====

            【釣魚短訊類】
            - 假冒菜鳥快遞：名字一樣「Cainiao」、釣魚短訊、點擊連結
            - 假冒水務署：假水費單、名稱帶「WSD」「GOV」未必為真
            - 支付寶「YAS」假扣費：YAS、扣費、驚慌轉帳
            - 「中銀生活」釣魚短訊：訂閱服務到期、即日扣月費
            - 假冒YouTube Premium：續訂、已扣款
            - 假冒IBKR/uSMART：W-8 BEN表格屆滿、表格更新
            - 假冒foodpanda/Keeta：冇訂餐但收到扣款短訊
            - 假冒快遞(拼多多)：釣魚短訊、中年婦損失320萬
            - 假冒順豐：新一波假冒順豐短訊
            - 假冒滙豐/香港郵政/抖音：虛假扣費釣魚短訊
            - 假冒Now TV/HOY TV：已續訂Netflix收取高額月費

            ===== CyberDefender「流行騙局排行榜」最新手法 (2026) =====

            【釣魚短訊類】
            - 假冒會籍釣魚：會籍即將收費、按指示致電騙徒、虛假網站
            - 假冒水務署(WSD)：水費逾期未交、虛假繳款網址
            - WhatsApp騎劫：點擊連結、提供驗證碼、帳戶被騎劫
            - 假冒支付寶(Alipay)：Alipay扣費、聯絡騙徒、虛假網站
            - 假冒滴滴(DiDi)：DiDi通知、按指示致電、虛假平台
            - 假冒旋轉拍賣(Carousell)：商品賣出、虛假收款網址
            - 假冒銀聯(UnionPay)：UnionPay通知、按指示致電、虛假網站
            - Telegram騎劫：Telegram、轉移代碼、帳戶被騎劫
            - 假冒快遞：快遞通知、按指示致電、虛假網址
            - 假冒活動門票：活動門票、按指示致電、虛假網址

            ===== 最新釣魚短訊樣本一覽 (政府/公共機構) =====
            - 假冒電子交通告票平台：「【電子告票】你的車輛於2026-01-20在XX路有違例記錄，請即到以下連結查閱詳情及繳付罰款」
            - 假冒水務署截水騙局：「【水務署】你的帳戶尚有水費未繳，為免被截水，請即日經由以下連結付款」
            - 假冒香港郵政包裹詐騙：「【香港郵政】你的包裹因地址錯誤無法派送，請於24小時內點擊連結更新地址」

            ===== 最新釣魚短訊樣本一覽 (金融/銀行) =====
            - 假冒中銀生活訂閱騙局：「【BOC Life】你的中銀生活訂閱服務已到期，即日將扣除月費$XXX，如需取消請致電XXXX XXXX」
            - 假冒銀行帳戶凍結：「【HSBC】發現你的銀行帳戶有異常交易，你的帳戶將被凍結，請立即登入以下連結進行驗證」
            - 假冒投資平台表格更新：「【IBKR】你的W-8 BEN表格已屆滿，請即更新個人資料，以免帳戶受限」

            ===== 最新釣魚短訊樣本一覽 (物流/電商) =====
            - 假冒順豐海關稅款騙局：「【順豐速運】你的快件(SF1028xxx)已到達，但海關檢查需補交稅款$5.5，請在24小時內處理」
            - 假冒HKTVmall退款騙局：「【HKTVmall】你的訂單#ABC123在付款時出現問題，我們將為你安排退款，請按以下連結填寫退款資料」
            - 假冒淘寶退款騙局：以退款或重複收款為由套取信用卡資料

            ===== 最新釣魚短訊樣本一覽 (媒體/通訊) =====
            - 假冒YouTube Premium訂閱：「【YouTube】多謝你訂閱YouTube Premium，你的信用卡今日被扣除月費$XXX，如非本人操作，請致電XXXX XXXX取消」
            - 假冒WhatsApp帳戶停用：「【WhatsApp】你的帳戶存在安全風險，將被停用。請點擊以下連結驗證帳戶」
            - 假冒WeChat帳戶安全：引導到偽冒登入頁面

            ===== 最新釣魚短訊樣本一覽 (生活/消閒) =====
            - 假冒yuu積分到期：「【yuu】你的積分將於日內到期，請即兌換獎賞，逾期作廢」
            - 假冒萬寧積分：積分到期、兌換連結
            - 假冒電子利是騙局：「[朋友名稱] 恭喜發財！我整左個電子利是俾你，快啲撳入黎拎啦」引導下載惡意APK
            - 假冒WhatsApp貼圖騙局：節日陷阱連結

            【投資騙案類】
            - Meta廣告投資騙案：80%投資騙案從Meta廣告開始
            - 假冒中電職員：誘騙投資環保廢料、國家電網環保廢料
            - 假冒港燈職員：誘騙投資國家電網環保廢料
            - 300%回報引誘：專業人士被騙780萬
            - 假AI投資App「Better-GC」：扮買股票逆市勁賺
            - 假冒富途牛牛/盈透證券：操控你帳戶買股輸鑊甘
            - 社交媒體成詐騙溫牀：Facebook假冒投資達人、拉人入投資群組

            【假冒官員類】
            - 假冒大學教授/職員：向學生借錢、受害人包括博士生、多間大學
            - 假冒內地旅遊局：聲稱「你被盜用回鄉證」
            - 假冒保安局：聲稱市民犯法、要求提供個人資料
            - 假冒庫務署：要求繳交罰款、附連結
            - 假冒人口普查統計員：無須提供身份證等敏感資料
            - 假冒官員新話術：指控你「散播大埔火災消息」或「在內地非法籌款」
            - 假冒中國移動職員：長者多被騙到櫃員機轉帳
            - 假冒反詐騙協調中心警員：有人假冒ADCC警員
            - 假冒通訊事務管理局/數字政策辦公室
            - 假警服．假公安陷阱：留學研究生痛失逾100萬

            【假冒身份類】
            - 假冒ADCC二次詐騙：WhatsApp你盡快辦理回款？是騙徒假冒ADCC
            - 假冒警員：反詐騙協調中心人員絕不會發送委任證照片
            - 假冒財務公司呃還錢：應先查清是否「真．欠債」
            - 假冒校長：扮校長呃學校職員、多間學校中招
            - 漏水情緣：騙徒假扮鄰居詐騙
            - 假冒AIA：釣魚電郵／短訊引你點入假網站
            - 永安平機票騙局：一周內爆逾20宗

            【刷單騙案類】
            - 刷單任務＝破產之路：教你看穿3伎倆
            - 假冒Trip.com/Agoda/Alibaba：開刷單群組、點讚有錢賺
            - 呃人WhatsApp群組：多人被氹投資損失逾百萬

            【演唱會門票騙案】
            - 周杰倫演唱會：7大購票陷阱、Carousell及Facebook平台頻出現

            【假冒快遞/服務】
            - 不明快遞宣稱中獎：勿掃二維碼
            - 順豐更改通知：SFHK APP推送快件自取通知（從此不發SMS！）

            【跨境騙案】
            - 假冒印度警察：在港尼泊爾人注意
            - 假冒港鐵（深圳）職員：北上留意
            - 假冒深圳民政局：唔會打俾你
            - 一咭三地電話卡？小心騙案

            【交通/罰款類】
            - 告票電子化：記得要睇有無「#」號開首！
            - 騙取交通意外傷亡援助金：難逃法網

            【偽鈔類】
            - 快速分辨500元/1000元偽鈔

            【虛假捐款類】
            - 虛假捐款確認通知：騙徒冒充特區政府發出釣魚短訊
            - 警惕一切以大埔火災為名的騙局

            ===== 分析指引 =====
            請根據以上ADCC、CyberDefender及最新釣魚短訊樣本資料庫，判斷用戶訊息屬於邊種詐騙手法。

            請以 JSON 格式回覆：
            {
                "riskLevel": "HIGH/MEDIUM/LOW",
                "scamType": "釣魚短訊/投資騙案/假冒官員/假冒身份/刷單騙案/演唱會門票/假冒快遞/假冒銀行/租房騙案/跨境騙案/交通罰款/假冒電視台/虛假捐款/偽鈔/帳戶騎劫/惡意程式/其他",
                "matchedCase": "匹配到嘅具體案例名稱",
                "advice": ["建議1", "建議2"],
                "explanation": "根據ADCC/CyberDefender/釣魚樣本資料，呢個訊息屬於..."
            }

            必須使用繁體中文。
            """;

        // 2. 構建請求
        ChatRequest chatRequest = new ChatRequest(
                model,
                List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", userMessage)
                ),
                0.3,
                500
        );

        // 3. 調用 DeepSeek API
        return openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(chatRequest)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(response -> {
                    String content = response.getFirstReplyContent();
                    System.out.println("======= AI RAW RESPONSE =======");
                    System.out.println(content);
                    System.out.println("================================");

                    try {
                        // 嘗試解析 JSON
                        Map<String, Object> aiResult = objectMapper.readValue(content, Map.class);
                        System.out.println("成功解析 AI 回應: " + aiResult);

                        // 儲存查詢記錄到 MySQL
                        try {
                            QueryRecord record = new QueryRecord(
                                    userMessage,
                                    (String) aiResult.get("riskLevel"),
                                    (String) aiResult.get("scamType"),
                                    String.join(", ", (List<String>) aiResult.get("advice"))
                            );
                            queryRecordRepository.save(record);
                            System.out.println("✅ 查詢記錄已儲存到數據庫");
                        } catch (Exception e) {
                            System.err.println("❌ 儲存記錄失敗: " + e.getMessage());
                        }

                        return aiResult;
                    } catch (Exception e) {
                        System.err.println("JSON 解析失敗: " + e.getMessage());
                        e.printStackTrace();
                        // 如果解析失敗，用 fallback
                        return fallbackAnalyze(userMessage);
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("AI API Error: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.just(fallbackAnalyze(userMessage));
                });
    }

    // ===== OCR 文字修復 API =====
    @PostMapping("/clean-ocr")
    public Mono<Map<String, String>> cleanOcrText(@RequestBody Map<String, String> request) {
        String rawText = request.get("text");

        String cleanPrompt = """
            你係一位專業嘅文字修復助手，專門處理 OCR 識別錯誤。
            
            ===== OCR 常見錯誤類型 =====
            
            1. 【空格問題】中文字之間出現不必要嘅空格
               例子：「你 好 嗎」→「你好嗎」
               
            2. 【字形混淆】形狀相似嘅字被認錯
               請根據上下文推測正確嘅字
            
            3. 【標點符號錯誤】全形標點被認錯
               例子：「˙」→「。」或「，」，「﹕」→「：」
            
            4. 【字母數字混淆】例如 O 同 0、l 同 1
               注意：電話號碼入面嘅數字要保留原樣
            
            ===== 修復原則 =====
            
            1. 刪除中文字之間嘅多餘空格
            2. 根據上下文推測正確嘅字詞
            3. 保持電話號碼、金額、日期嘅數字正確
            4. 修復後嘅句子必須通順合理
            5. 如果不確定，保留原文
            
            ===== 例子 =====
            
            輸入：「蒂 敬 的 用 戶 ， 您 所 訂 的 會 員 服 務 已 續 約」
            輸出：「尊敬的用戶，您所訂的會員服務已續約」
            
            輸入：「升 始 收 單 1560HKD ， 如 需 取 消 致 電 服 務 熱 縫 ﹕ 90323732 x」
            輸出：「開始收費 1560HKD，如需取消致電服務熱線：90323732」
            
            輸入：「帳 戶 異 常 請 點 擊 連 結 h t t p : / / t e s t . x y z」
            輸出：「帳戶異常請點擊連結 http://test.xyz」
            
            ===== 現在請修復以下文字 =====
            
            %s
            """.formatted(rawText);

        ChatRequest chatRequest = new ChatRequest(
                model,
                List.of(new ChatMessage("user", cleanPrompt)),
                0.2,
                500
        );

        return openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(chatRequest)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(response -> {
                    String cleaned = response.getFirstReplyContent();
                    cleaned = cleaned.trim();
                    return Map.of("cleanedText", cleaned);
                })
                .onErrorResume(e -> {
                    System.err.println("OCR clean error: " + e.getMessage());
                    return Mono.just(Map.of("cleanedText", rawText));
                });
    }

    // Fallback 分析（根據ADCC + CyberDefender + 最新釣魚短訊樣本資料庫）
    private Map<String, Object> fallbackAnalyze(String message) {
        Map<String, Object> response = new HashMap<>();

        // 定義詐騙手法匹配規則（根據ADCC + CyberDefender + 最新釣魚樣本）
        Map<String, Map<String, Object>> scamDatabase = new HashMap<>();

        // ===== 1. 釣魚短訊類 (ADCC) =====
        scamDatabase.put("假冒菜鳥快遞", Map.of(
                "keywords", List.of("cainiao", "菜鳥", "快遞通知", "點擊連結"),
                "type", "釣魚短訊",
                "advice", List.of("❌ 假冒菜鳥釣魚短訊！", "❌ 名字一樣但URL唔同", "✅ 查閱官方App"))
        );

        scamDatabase.put("假冒水務署", Map.of(
                "keywords", List.of("水費單", "wsd", "gov", "水務署", "#"),
                "type", "釣魚短訊",
                "advice", List.of("❌ 假水費單短訊！", "❌ 名稱帶WSD/GOV未必為真", "✅ 登入官方網站查閱"))
        );

        scamDatabase.put("支付寶YAS假扣費", Map.of(
                "keywords", List.of("支付寶", "yas", "扣費", "轉帳", "驚慌"),
                "type", "釣魚短訊",
                "advice", List.of("❌ 支付寶YAS假扣費騙局！", "❌ 唔好驚慌轉帳", "✅ 登入支付寶App核實"))
        );

        scamDatabase.put("中銀生活釣魚短訊", Map.of(
                "keywords", List.of("中銀", "訂閱服務", "到期", "扣月費", "中銀生活"),
                "type", "釣魚短訊",
                "advice", List.of("❌ 假冒中銀釣魚短訊！", "❌ 銀行唔會經SMS叫你點擊連結", "✅ 打銀行熱線核實"))
        );

        scamDatabase.put("假冒YouTube Premium", Map.of(
                "keywords", List.of("youtube", "premium", "續訂", "已扣款", "訂閱"),
                "type", "釣魚短訊",
                "advice", List.of("❌ 假冒YouTube Premium！", "❌ 唔好點擊連結", "✅ 登入Google帳戶檢查"))
        );

        scamDatabase.put("假冒IBKR/uSMART", Map.of(
                "keywords", List.of("ibkr", "usmart", "w-8", "ben", "表格屆滿", "表格更新"),
                "type", "釣魚短訊",
                "advice", List.of("❌ 假冒投資平台短訊！", "❌ 表格更新唔會經SMS", "✅ 登入官方平台檢查"))
        );

        scamDatabase.put("假冒foodpanda/Keeta", Map.of(
                "keywords", List.of("foodpanda", "keeta", "扣款", "冇訂餐", "訂單"),
                "type", "釣魚短訊",
                "advice", List.of("❌ 假冒外賣平台扣款！", "❌ 冇訂餐就唔使理", "✅ 登入App檢查訂單"))
        );

        scamDatabase.put("假冒順豐", Map.of(
                "keywords", List.of("順豐", "sfhk", "快件", "自取通知", "sf"),
                "type", "釣魚短訊",
                "advice", List.of("❌ 假冒順豐短訊！", "❌ 順豐已改由App推送通知", "✅ 下載官方SFHK App"))
        );

        scamDatabase.put("假冒Now TV/HOY TV", Map.of(
                "keywords", List.of("now tv", "hoy tv", "netflix", "續訂", "高額月費", "電視台"),
                "type", "假冒電視台",
                "advice", List.of("❌ 假冒電視台續訂騙局！", "❌ 冇訂過就唔使理", "✅ 直接向電視台查詢"))
        );

        // ===== 2. 投資騙案類 (ADCC) =====
        scamDatabase.put("Meta廣告投資騙案", Map.of(
                "keywords", List.of("meta", "facebook", "廣告", "投資", "群組", "達人"),
                "type", "投資騙案",
                "advice", List.of("❌ 80%投資騙案從Meta廣告開始！", "❌ 唔好信社交媒體投資廣告", "✅ 查閱證監會網站"))
        );

        scamDatabase.put("假冒中電投資環保廢料", Map.of(
                "keywords", List.of("中電", "環保廢料", "國家電網", "廢料", "投資", "社交媒體"),
                "type", "投資騙案",
                "advice", List.of("❌ 假冒中電投資騙局！", "❌ 中電唔會經社交媒體推廣投資", "✅ 直接打中電熱線核實"))
        );

        scamDatabase.put("假冒港燈投資", Map.of(
                "keywords", List.of("港燈", "國家電網環保廢料", "環保廢料", "投資"),
                "type", "投資騙案",
                "advice", List.of("❌ 假冒港燈投資騙局！", "❌ 電力公司唔會叫你投資廢料", "✅ 直接打港燈熱線"))
        );

        scamDatabase.put("300%回報投資", Map.of(
                "keywords", List.of("300%", "高回報", "專業人士", "被騙", "保證回報"),
                "type", "投資騙案",
                "advice", List.of("❌ 300%回報係騙局！", "❌ 咁高回報一定係騙局", "✅ 投資前查閱證監會警告名單"))
        );

        scamDatabase.put("假AI投資App Better-GC", Map.of(
                "keywords", List.of("better-gc", "better gc", "ai投資", "逆市勁賺", "扮買股票"),
                "type", "投資騙案",
                "advice", List.of("❌ 假AI投資App Better-GC！", "❌ 聲稱逆市勁賺都係假", "✅ 查閱證監會網站"))
        );

        scamDatabase.put("假冒富途牛牛", Map.of(
                "keywords", List.of("富途", "富途牛牛", "盈透", "操控帳戶", "買股輸錢"),
                "type", "投資騙案",
                "advice", List.of("❌ 假冒富途牛牛騙局！", "❌ 唔好俾任何人操控你帳戶", "✅ 登入官方平台檢查"))
        );

        // ===== 3. 假冒官員類 (ADCC) =====
        scamDatabase.put("假冒大學教授借錢", Map.of(
                "keywords", List.of("教授", "大學", "博士生", "借錢", "學生"),
                "type", "假冒官員",
                "advice", List.of("❌ 假冒大學教授借錢！", "❌ 教授唔會問學生借錢", "✅ 直接去學校辦公室核實"))
        );

        scamDatabase.put("假冒內地旅遊局", Map.of(
                "keywords", List.of("旅遊局", "回鄉證", "盜用", "內地"),
                "type", "假冒官員",
                "advice", List.of("❌ 假冒內地旅遊局！", "❌ 政府部門唔會電話通知你", "✅ 收線後自行查詢"))
        );

        scamDatabase.put("假冒保安局", Map.of(
                "keywords", List.of("保安局", "犯法", "個人資料", "調查"),
                "type", "假冒官員",
                "advice", List.of("❌ 假冒保安局電話騙案！", "❌ 政府唔會電話要求俾個人資料", "✅ 立即收線"))
        );

        scamDatabase.put("假冒庫務署", Map.of(
                "keywords", List.of("庫務署", "罰款", "繳交", "連結"),
                "type", "假冒官員",
                "advice", List.of("❌ 假冒庫務署罰款短訊！", "❌ 政府罰款唔會經SMS", "✅ 打庫務署熱線核實"))
        );

        scamDatabase.put("假冒人口普查統計員", Map.of(
                "keywords", List.of("人口普查", "統計員", "身份證", "敏感資料"),
                "type", "假冒官員",
                "advice", List.of("❌ 假冒人口普查統計員！", "❌ 普查員無須提供身份證", "✅ 查閱政府公告"))
        );

        scamDatabase.put("假冒官員新話術-火災", Map.of(
                "keywords", List.of("大埔火災", "散播消息", "非法籌款", "火災"),
                "type", "假冒官員",
                "advice", List.of("❌ 假冒官員新話術！", "❌ 利用災害行騙", "✅ 唔好信電話指控"))
        );

        scamDatabase.put("假冒中國移動", Map.of(
                "keywords", List.of("中國移動", "中移動", "櫃員機", "長者", "轉帳"),
                "type", "假冒官員",
                "advice", List.of("❌ 假冒中國移動職員！", "❌ 電訊商唔會叫你去櫃員機", "✅ 打客服熱線核實"))
        );

        scamDatabase.put("假冒反詐騙協調中心", Map.of(
                "keywords", List.of("adcc", "反詐騙", "協調中心", "警員", "委任證"),
                "type", "假冒官員",
                "advice", List.of("❌ 有人假冒ADCC警員！", "❌ ADCC唔會發送委任證照片", "✅ 打18222核實"))
        );

        scamDatabase.put("假警服假公安", Map.of(
                "keywords", List.of("公安", "警服", "研究生", "留學", "痛失"),
                "type", "假冒官員",
                "advice", List.of("❌ 假警服假公安陷阱！", "❌ 公安唔會電話辦案", "✅ 立即收線"))
        );

        // ===== 4. 假冒身份類 (ADCC) =====
        scamDatabase.put("假冒ADCC二次詐騙", Map.of(
                "keywords", List.of("追回騙款", "辦理回款", "whatsapp", "盡快辦理", "二次詐騙"),
                "type", "假冒身份",
                "advice", List.of("❌ 假冒ADCC二次詐騙！", "❌ ADCC唔會經WhatsApp叫你辦理回款", "✅ 有疑問打18222"))
        );

        scamDatabase.put("假冒財務公司呃還錢", Map.of(
                "keywords", List.of("財務公司", "還錢", "欠債", "真欠債"),
                "type", "假冒身份",
                "advice", List.of("❌ 假冒財務公司呃還錢！", "❌ 先查清係咪真欠債", "✅ 同真正財務公司核實"))
        );

        scamDatabase.put("假冒校長", Map.of(
                "keywords", List.of("校長", "學校", "職員", "借錢", "中招"),
                "type", "假冒身份",
                "advice", List.of("❌ 假冒校長呃學校職員！", "❌ 校長唔會問職員借錢", "✅ 直接去校長室核實"))
        );

        scamDatabase.put("漏水情緣", Map.of(
                "keywords", List.of("漏水", "鄰居", "假扮", "情緣", "騙徒"),
                "type", "假冒身份",
                "advice", List.of("❌ 漏水情緣騙局！", "❌ 小心假扮鄰居嘅騙徒", "✅ 核實對方身份"))
        );

        scamDatabase.put("假冒AIA", Map.of(
                "keywords", List.of("aia", "保險", "釣魚電郵", "假網站"),
                "type", "假冒身份",
                "advice", List.of("❌ 假冒AIA釣魚電郵！", "❌ 唔好點入連結", "✅ 登入官方網站"))
        );

        scamDatabase.put("永安平機票騙局", Map.of(
                "keywords", List.of("永安", "平機票", "機票", "騙局", "旅行社"),
                "type", "假冒身份",
                "advice", List.of("❌ 永安平機票騙局！", "❌ 超平機票要小心", "✅ 經官方渠道購買"))
        );

        // ===== 5. 刷單騙案類 (ADCC) =====
        scamDatabase.put("刷單任務破產之路", Map.of(
                "keywords", List.of("刷單", "任務", "破產", "兼職", "佣金"),
                "type", "刷單騙案",
                "advice", List.of("❌ 刷單任務＝破產之路！", "❌ 刷單賺佣都係騙局", "✅ 正當工作唔使墊支"))
        );

        scamDatabase.put("假冒旅遊平台刷單", Map.of(
                "keywords", List.of("trip.com", "agoda", "alibaba", "點讚", "刷單群組", "有錢賺"),
                "type", "刷單騙案",
                "advice", List.of("❌ 假冒旅遊平台刷單群組！", "❌ 點讚有錢賺都係假", "✅ 唔好入不明群組"))
        );

        scamDatabase.put("呃人WhatsApp群組", Map.of(
                "keywords", List.of("whatsapp群組", "被氹投資", "損失百萬", "投資群組"),
                "type", "刷單騙案",
                "advice", List.of("❌ 呃人WhatsApp群組！", "❌ 唔好信群組內嘅投資貼士", "✅ 退出群組"))
        );

        // ===== 6. 演唱會門票騙案 (ADCC) =====
        scamDatabase.put("周杰倫演唱會門票", Map.of(
                "keywords", List.of("周杰倫", "演唱會", "門票", "讓票", "carousell", "facebook", "購票陷阱"),
                "type", "演唱會門票",
                "advice", List.of("❌ 周杰倫演唱會門票騙局！", "❌ 7大購票陷阱要小心", "✅ 經官方渠道購買"))
        );

        // ===== 7. 假冒快遞/服務 (ADCC) =====
        scamDatabase.put("不明快遞中獎", Map.of(
                "keywords", List.of("不明快遞", "中獎", "二維碼", "掃碼"),
                "type", "假冒快遞",
                "advice", List.of("❌ 不明快遞中獎騙局！", "❌ 唔好掃不明二維碼", "✅ 直接丟掉"))
        );

        // ===== 8. 跨境騙案 (ADCC) =====
        scamDatabase.put("假冒印度警察", Map.of(
                "keywords", List.of("印度警察", "尼泊爾人", "在港"),
                "type", "跨境騙案",
                "advice", List.of("❌ 假冒印度警察騙案！", "❌ 在港尼泊爾人要小心", "✅ 收線後報警"))
        );

        scamDatabase.put("假冒港鐵深圳", Map.of(
                "keywords", List.of("港鐵", "深圳", "職員", "北上"),
                "type", "跨境騙案",
                "advice", List.of("❌ 假冒港鐵（深圳）職員！", "❌ 北上留意呢類電話", "✅ 打港鐵熱線核實"))
        );

        scamDatabase.put("假冒深圳民政局", Map.of(
                "keywords", List.of("深圳", "民政局", "唔會打俾你"),
                "type", "跨境騙案",
                "advice", List.of("❌ 假冒深圳民政局！", "❌ 深圳民政局唔會電話通知你", "✅ 收線"))
        );

        scamDatabase.put("一咭三地電話卡", Map.of(
                "keywords", List.of("一咭三地", "電話卡", "小心騙案"),
                "type", "跨境騙案",
                "advice", List.of("❌ 一咭三地電話卡騙局！", "❌ 申請要經正規渠道", "✅ 去門市辦理"))
        );

        // ===== 9. 交通/罰款類 (ADCC) =====
        scamDatabase.put("告票電子化", Map.of(
                "keywords", List.of("告票", "電子化", "#", "罰款", "#號開首"),
                "type", "交通罰款",
                "advice", List.of("❌ 告票電子化防騙須知！", "❌ 留意有無「#」號開首", "✅ 查閱政府公告"))
        );

        scamDatabase.put("交通意外傷亡援助金騙案", Map.of(
                "keywords", List.of("交通意外", "傷亡援助金", "援助金", "騙取"),
                "type", "交通罰款",
                "advice", List.of("❌ 騙取交通意外傷亡援助金！", "❌ 申請援助要如實申報", "✅ 騙取援助金難逃法網"))
        );

        // ===== 10. 偽鈔類 (ADCC) =====
        scamDatabase.put("快速分辨偽鈔", Map.of(
                "keywords", List.of("偽鈔", "500元", "1000元", "分辨", "鈔票"),
                "type", "偽鈔",
                "advice", List.of("❌ 小心偽鈔！", "❌ 學識快速分辨500/1000元偽鈔", "✅ 收錢時留意"))
        );

        // ===== 11. 虛假捐款類 (ADCC) =====
        scamDatabase.put("虛假捐款確認通知", Map.of(
                "keywords", List.of("捐款", "確認通知", "特區政府", "釣魚短訊"),
                "type", "虛假捐款",
                "advice", List.of("❌ 虛假捐款確認通知！", "❌ 冒充特區政府釣魚短訊", "✅ 唔好點連結"))
        );

        scamDatabase.put("大埔火災為名騙局", Map.of(
                "keywords", List.of("大埔火災", "捐款", "騙局", "警惕"),
                "type", "虛假捐款",
                "advice", List.of("❌ 警惕一切以大埔火災為名的騙局！", "❌ 捐錢要去正規機構", "✅ 查閱官方捐款渠道"))
        );

        // ===== 12. CyberDefender最新騙案排行榜 (2026) =====
        scamDatabase.put("假冒會籍釣魚", Map.of(
                "keywords", List.of("會籍", "即將收費", "致電", "虛假網站", "會費", "會員", "到期"),
                "type", "釣魚短訊",
                "advice", List.of(
                        "❌ 假冒會籍釣魚短訊！",
                        "❌ 會籍續期唔會經SMS叫你致電",
                        "✅ 登入官方網站或App查詢",
                        "📞 有疑問打返客服熱線"))
        );

        scamDatabase.put("假冒水務署WSD", Map.of(
                "keywords", List.of("wsd", "水務署", "水費逾期", "逾期未交", "水費", "繳款", "虛假網址"),
                "type", "釣魚短訊",
                "advice", List.of(
                        "❌ 假冒水務署釣魚短訊！",
                        "❌ 水費逾期唔會經SMS叫你點連結",
                        "✅ 登入水務署網站查詢",
                        "📞 有疑問打水務署熱線"))
        );

        scamDatabase.put("WhatsApp騎劫", Map.of(
                "keywords", List.of("whatsapp", "驗證碼", "點擊連結", "帳戶騎劫", "whatsapp群組", "通訊錄", "家人", "朋友"),
                "type", "帳戶騎劫",
                "advice", List.of(
                        "❌ WhatsApp騎劫騙局！",
                        "❌ 唔好將驗證碼俾任何人",
                        "❌ 唔好點擊不明連結",
                        "✅ 開啟WhatsApp雙重驗證"))
        );

        scamDatabase.put("假冒支付寶Alipay", Map.of(
                "keywords", List.of("alipay", "支付寶", "扣費", "致電", "虛假網站", "客服"),
                "type", "釣魚短訊",
                "advice", List.of(
                        "❌ 假冒支付寶釣魚短訊！",
                        "❌ 支付寶扣費通知請喺App查閱",
                        "✅ 直接打開支付寶App核實"))
        );

        scamDatabase.put("假冒滴滴DiDi", Map.of(
                "keywords", List.of("didi", "滴滴", "出行", "致電", "虛假網站", "客服"),
                "type", "釣魚短訊",
                "advice", List.of(
                        "❌ 假冒滴滴釣魚短訊！",
                        "❌ 唔好信SMS叫你致電",
                        "✅ 打開滴滴App查詢"))
        );

        scamDatabase.put("假冒旋轉拍賣Carousell", Map.of(
                "keywords", List.of("carousell", "旋轉拍賣", "商品賣出", "收款", "虛假網址", "買家"),
                "type", "釣魚短訊",
                "advice", List.of(
                        "❌ 假冒Carousell釣魚短訊！",
                        "❌ 賣出商品嘅通知喺App入面",
                        "✅ 登入CarousellApp檢查"))
        );

        scamDatabase.put("假冒銀聯UnionPay", Map.of(
                "keywords", List.of("unionpay", "銀聯", "致電", "虛假網站", "信用卡", "銀行"),
                "type", "釣魚短訊",
                "advice", List.of(
                        "❌ 假冒銀聯釣魚短訊！",
                        "❌ 銀行唔會經SMS叫你致電",
                        "✅ 打信用卡背面熱線核實"))
        );

        scamDatabase.put("Telegram騎劫", Map.of(
                "keywords", List.of("telegram", "轉移代碼", "帳戶騎劫", "驗證碼", "tg", "登入"),
                "type", "帳戶騎劫",
                "advice", List.of(
                        "❌ Telegram騎劫騙局！",
                        "❌ 唔好將轉移代碼俾任何人",
                        "✅ 開啟Telegram雙重驗證"))
        );

        scamDatabase.put("假冒快遞釣魚", Map.of(
                "keywords", List.of("快遞", "快件", "包裹", "致電", "虛假網址", "sf", "順豐", "速遞"),
                "type", "釣魚短訊",
                "advice", List.of(
                        "❌ 假冒快遞釣魚短訊！",
                        "❌ 快遞通知請喺官方App查閱",
                        "✅ 用官方App追蹤快件"))
        );

        scamDatabase.put("假冒活動門票", Map.of(
                "keywords", List.of("門票", "活動", "演唱會", "致電", "虛假網址", "購票", "訂票"),
                "type", "釣魚短訊",
                "advice", List.of(
                        "❌ 假冒活動門票釣魚短訊！",
                        "❌ 買飛要去官方渠道",
                        "✅ 直接去官網或購票App"))
        );

        // 開始匹配
        List<String> matchedCases = new ArrayList<>();
        String matchedType = "其他";
        int maxKeywords = 0;
        List<String> bestAdvice = List.of(
                "✅ 沒有發現明顯詐騙特徵",
                "💡 但仍需保持警惕",
                "📞 防騙易熱線：18222"
        );

        for (Map.Entry<String, Map<String, Object>> entry : scamDatabase.entrySet()) {
            String caseName = entry.getKey();
            Map<String, Object> caseData = entry.getValue();
            List<String> keywords = (List<String>) caseData.get("keywords");

            int matchCount = 0;
            for (String keyword : keywords) {
                if (message.toLowerCase().contains(keyword.toLowerCase())) {
                    matchCount++;
                }
            }

            if (matchCount > 0) {
                matchedCases.add(caseName + " (" + matchCount + "個特徵)");
                if (matchCount > maxKeywords) {
                    maxKeywords = matchCount;
                    matchedType = (String) caseData.get("type");
                    response.put("matchedCase", caseName);
                    bestAdvice = (List<String>) caseData.get("advice");
                }
            }
        }

        // 決定風險等級
        if (maxKeywords >= 2) {
            response.put("riskLevel", "HIGH");
        } else if (maxKeywords == 1) {
            response.put("riskLevel", "MEDIUM");
        } else {
            response.put("riskLevel", "LOW");
            response.put("matchedCase", "無匹配案例");
        }

        response.put("scamType", matchedType);
        response.put("matchedPatterns", matchedCases);
        response.put("advice", bestAdvice);
        response.put("confidence", 0.85);

        // 儲存查詢記錄到 MySQL（fallback）
        try {
            QueryRecord record = new QueryRecord(
                    message,
                    (String) response.get("riskLevel"),
                    (String) response.get("scamType"),
                    String.join(", ", (List<String>) response.get("advice"))
            );
            queryRecordRepository.save(record);
            System.out.println("✅ 查詢記錄已儲存到數據庫（fallback）");
        } catch (Exception e) {
            System.err.println("❌ 儲存記錄失敗: " + e.getMessage());
        }

        return response;
    }
}