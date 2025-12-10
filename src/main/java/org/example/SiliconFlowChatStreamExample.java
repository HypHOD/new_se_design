import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SiliconFlowChatStreamExample {
    // 注意：实际项目中不要硬编码API Key，建议用环境变量/配置文件
    private static final String API_KEY = "sk-tkxomihgmxmxxyvwcmratnoiqtjnpqsvqvdzcsscdihfcjsa";
    private static final String BASE_URL = "https://api.siliconflow.cn/v1/chat/completions";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        // 1. 创建OkHttp客户端
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .build();

        // 2. 构建请求体（与Python代码的参数一致）
        String requestBodyJson = "{" +
                "\"model\": \"deepseek-ai/DeepSeek-OCR\"," + // 注意：该模型是OCR模型，若要回答推理问题建议换模型（如deepseek-chat）
                "\"messages\": [{\"role\": \"user\", \"content\": \"推理模型会给市场带来哪些新的机会\"}]," +
                "\"stream\": true" +
                "}";

        RequestBody requestBody = RequestBody.create(
                requestBodyJson,
                MediaType.parse("application/json; charset=utf-8"));

        // 3. 构建HTTP请求（设置Authorization、Content-Type等头）
        Request request = new Request.Builder()
                .url(BASE_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

        // 4. 发送请求并处理流式响应
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败，状态码：" + response.code() + "，信息：" + response.message());
            }

            // 读取SSE格式的响应流（每行以"data: "开头）
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 忽略空行或注释行
                    if (line.isEmpty() || line.startsWith(":")) {
                        continue;
                    }
                    // 提取SSE的data部分（去掉"data: "前缀）
                    if (line.startsWith("data: ")) {
                        String data = line.substring("data: ".length().trim());
                        // 处理流式结束信号（data: [DONE]）
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        // 反序列化为自定义Chunk类（适配reasoning_content字段）
                        CustomChatCompletionChunk chunk = OBJECT_MAPPER.readValue(data,
                                CustomChatCompletionChunk.class);
                        // 打印内容
                        processChunk(chunk);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 处理单个Chunk，打印content和reasoning_content
    private static void processChunk(CustomChatCompletionChunk chunk) {
        if (chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
            return;
        }
        CustomChatCompletionChunk.CustomChoice choice = chunk.getChoices().get(0);
        CustomDelta delta = choice.getDelta();
        if (delta == null) {
            return;
        }
        // 打印content
        if (delta.getContent() != null) {
            System.out.print(delta.getContent());
        }
        // 打印reasoning_content（SiliconFlow的扩展字段）
        if (delta.getReasoningContent() != null) {
            System.out.print(delta.getReasoningContent());
        }
        // 强制刷新输出流（避免内容堆积）
        System.out.flush();
    }

    // 自定义Chunk类：适配SiliconFlow的响应格式（包含reasoning_content扩展字段）
    static class CustomChatCompletionChunk {
        @JsonProperty("choices")
        private java.util.List<CustomChoice> choices;

        public java.util.List<CustomChoice> getChoices() {
            return choices;
        }

        public void setChoices(java.util.List<CustomChoice> choices) {
            this.choices = choices;
        }

        // 自定义Choice类
        static class CustomChoice {
            @JsonProperty("delta")
            private CustomDelta delta;

            public CustomDelta getDelta() {
                return delta;
            }

            public void setDelta(CustomDelta delta) {
                this.delta = delta;
            }
        }
    }

    // 自定义Delta类：包含OpenAI标准的content + SiliconFlow扩展的reasoning_content
    static class CustomDelta {
        @JsonProperty("content")
        private String content;

        @JsonProperty("reasoning_content")
        private String reasoningContent;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getReasoningContent() {
            return reasoningContent;
        }

        public void setReasoningContent(String reasoningContent) {
            this.reasoningContent = reasoningContent;
        }
    }
}