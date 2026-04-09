import json
import boto3
import os
import time
import re

# Initialize Bedrock client
bedrock = boto3.client(
    service_name='bedrock-runtime', 
    region_name=os.environ.get("AWS_REGION", "eu-west-2")
)

def lambda_handler(event, context):
    """
    AWS Lambda handler for document analysis.
    Expected event body: { "text": "...", "engine": "local" | "bedrock" }
    """
    try:
        # Handle both direct invocation and API Gateway proxy (though we'll likely use direct AWS SDK call)
        if 'body' in event:
            data = json.loads(event['body'])
        else:
            data = event
            
        text = data.get("text", "").strip()
        engine = data.get("engine", "local").lower()

        if not text:
            return {
                'statusCode': 400,
                'body': json.dumps({"error": "Field 'text' is required."})
            }

        start_time = time.time()
        word_count = len(text.split())
        
        if engine == "bedrock":
            analysis = _analyze_with_bedrock(text)
            engine_name = "aws-bedrock-claude-3-haiku"
        else:
            analysis = _analyze_locally(text)
            engine_name = "local-keyword-engine"
        
        processing_ms = int((time.time() - start_time) * 1000)

        result = {
            "summary":        analysis.get("summary"),
            "word_count":     word_count,
            "classification": analysis.get("classification"),
            "pii_entities":   analysis.get("pii_entities", []),
            "processing_ms":  processing_ms,
            "engine":         engine_name
        }

        return {
            'statusCode': 200,
            'body': json.dumps(result)
        }

    except Exception as e:
        print(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({"error": f"Analysis failed: {str(e)}"})
        }

def _analyze_locally(text: str) -> dict:
    text_lower = text.lower()
    
    # 1. Classification
    tech_kw = ["api", "server", "code", "system", "infrastructure"]
    legal_kw = ["agreement", "contract", "liability", "clause"]
    fin_kw = ["revenue", "profit", "budget", "invoice"]
    
    scores = {
        "technical": sum(1 for kw in tech_kw if kw in text_lower),
        "legal":     sum(1 for kw in legal_kw if kw in text_lower),
        "financial": sum(1 for kw in fin_kw if kw in text_lower),
    }
    best = max(scores, key=scores.get)
    classification = best if scores[best] > 0 else "general"

    # 2. Summary (First 3 sentences)
    sentences = re.split(r'(?<=[.!?])\s+', text.strip())
    summary = " ".join(sentences[:3]) if sentences else "No content."

    # 3. PII (Regex)
    patterns = {
        "email": r'[a-zA-Z0-9._%+-]+@+[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}',
        "phone": r'\b\d{3}[-.\s]??\d{3}[-.\s]??\d{4}\b'
    }
    pii = []
    for label, pattern in patterns.items():
        matches = re.findall(pattern, text)
        if matches:
            pii.append({"type": label, "count": len(matches)})

    return {
        "summary": summary,
        "classification": classification,
        "pii_entities": pii
    }

def _analyze_with_bedrock(text: str) -> dict:
    # Truncate text if too long
    max_input_length = 30000 
    truncated_text = text[:max_input_length]

    prompt = f"""
    Analyze the following document text and provide a structured JSON response.
    
    Tasks:
    1. Summarize the content in exactly 3 clear, professional sentences.
    2. Classify the document as one of: 'technical', 'legal', 'financial', or 'general'.
    3. Detect PII entities including 'email', 'phone', and 'credit_card'. 
       Return them as a list of objects with 'type' and 'count'.

    Text to analyze:
    {truncated_text}

    Response must be valid JSON only with no preamble:
    {{
      "summary": "...",
      "classification": "...",
      "pii_entities": [ {{"type": "...", "count": 0}}, ... ]
    }}
    """
    
    body = json.dumps({
        "anthropic_version": "bedrock-2023-05-31",
        "max_tokens": 1000,
        "temperature": 0,
        "messages": [
            {
                "role": "user",
                "content": prompt
            }
        ]
    })
    
    response = bedrock.invoke_model(
        modelId='anthropic.claude-3-haiku-20240307-v1:0',
        body=body
    )
    
    response_body = json.loads(response.get('body').read())
    result_text = response_body['content'][0]['text']
    
    try:
        return json.loads(result_text)
    except json.JSONDecodeError:
        start = result_text.find('{')
        end = result_text.rfind('}') + 1
        if start != -1 and end != 0:
            return json.loads(result_text[start:end])
        raise ValueError("Could not parse JSON from Bedrock response")
