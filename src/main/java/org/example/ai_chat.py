import sys
import json
import openai

# ========== 配置 DeepSeek API 信息 ==========
DEEPSEEK_API_KEY = "sk-uravkxhdaxzgoleppwxvdniatdkcfakljbhprjqrbjttkcvh"
DEEPSEEK_BASE_URL = "https://api.siliconflow.cn/v1"  # 你的服务器地址
MODEL_NAME = "deepseek-ai/DeepSeek-OCR"  # 对话模型（不要用OCR模型）

# 初始化客户端
client = openai.OpenAI(
    api_key=DEEPSEEK_API_KEY,
    base_url=DEEPSEEK_BASE_URL
)

def get_ai_reply(chat_history):
    """
    接收Java传递的完整对话历史，调用大模型返回回复
    :param chat_history: 列表，格式：[{"role":"user/assistant", "content":"消息内容"}]
    :return: AI回复字符串
    """
    try:
        # 调试输出：确认接收到的对话历史（Java端能在错误信息中看到）
        # print(f"[Python调试] 接收到的对话历史：{chat_history}", file=sys.stderr)

        # 调用DeepSeek API（直接传递完整对话历史，支持多轮对话）
        response = client.chat.completions.create(
            model=MODEL_NAME,
            messages=chat_history,  # 不再是单条消息，而是完整历史
            temperature=0.7,
            max_tokens=1024,
            timeout=30
        )

        # 提取回复
        if response.choices and len(response.choices) > 0:
            return response.choices[0].message.content.strip()
        else:
            return "AI未返回有效回复"

    except Exception as e:
        error_msg = f"[Python错误] 调用DeepSeek失败：[{type(e).__name__}] {str(e)}"
        print(error_msg, file=sys.stderr)  # 输出到错误流，Java会捕获
        return error_msg

if __name__ == "__main__":
    try:
        # ========== 关键修改：读取Java通过STDIN传递的JSON ==========
        # 1. 读取标准输入（Java写入的JSON），用UTF-8编码避免中文乱码
        input_json = sys.stdin.read().encode('utf-8').decode('utf-8').strip()
        
        # 2. 检查是否收到数据
        if not input_json:
            error_msg = "[Python错误] 未收到Java传递的对话历史（STDIN为空）"
            print(error_msg, file=sys.stderr)
            print(error_msg)
            sys.exit(1)
        
        # 3. 解析JSON为列表（Java传递的ChatMessage列表）
        chat_history = json.loads(input_json)
        
        # 4. 调用AI获取回复
        ai_reply = get_ai_reply(chat_history)
        
        # 5. 输出回复到标准输出（Java会读取此内容）
        print(ai_reply)

    except json.JSONDecodeError as e:
        # JSON格式错误（Java序列化失败）
        error_msg = f"[Python错误] JSON解析失败：{str(e)}，原始输入：{input_json}"
        print(error_msg, file=sys.stderr)
        print(error_msg)
        sys.exit(1)
    except Exception as e:
        # 其他未知错误
        error_msg = f"[Python错误] 脚本执行失败：[{type(e).__name__}] {str(e)}"
        print(error_msg, file=sys.stderr)
        print(error_msg)
        sys.exit(1)