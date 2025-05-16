from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import os
from dotenv import load_dotenv
from RtcTokenBuilder2 import RtcTokenBuilder, Role_Publisher, Role_Subscriber

load_dotenv()

app = FastAPI()

class TokenRequest(BaseModel):
    channelName: str
    uid: int
    role: str = "publisher"  # "publisher" or "subscriber"

@app.post("/get-token")
def get_token(request: TokenRequest):
    app_id = os.getenv("AGORA_APP_ID")
    app_cert = os.getenv("AGORA_APP_CERTIFICATE")

    if not app_id or not app_cert:
        raise HTTPException(status_code=500, detail="Agora credentials not configured")

    role_enum = Role_Publisher if request.role.lower() == "publisher" else Role_Subscriber

    token = RtcTokenBuilder.build_token_with_uid(
        app_id,
        app_cert,
        request.channelName,
        request.uid,
        role_enum,
        3600,  # token expiration
        3600   # privilege expiration
    )

    return {"token": token}
