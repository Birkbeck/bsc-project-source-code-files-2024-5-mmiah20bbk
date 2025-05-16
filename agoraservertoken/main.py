# import os
# import sys
#
# sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
#
# from src.RtcTokenBuilder2 import *
#
# def main():
#     # Get the value of the environment variable AGORA_APP_ID. Make sure you set this variable to the App ID you obtained from Agora console.
#     app_id = os.environ.get("AGORA_APP_ID")
#     # Get the value of the environment variable AGORA_APP_CERTIFICATE. Make sure you set this variable to the App certificate you obtained from Agora console
#     app_certificate = os.environ.get("AGORA_APP_CERTIFICATE")
#     # Replace channelName with the name of the channel you want to join
#     channel_name = "channel_name"
#     # Fill in your actual user ID
#     uid = 2882341273
#     # Token validity time in seconds
#     token_expiration_in_seconds = 3600
#     # The validity time of all permissions in seconds
#     privilege_expiration_in_seconds = 3600
#
#     print("App Id: %s" % app_id)
#     print("App Certificate: %s" % app_certificate)
#     if not app_id or not app_certificate:
#         print("Need to set environment variable AGORA_APP_ID and AGORA_APP_CERTIFICATE")
#         return
#
#     # Generate Token
#     token = RtcTokenBuilder.build_token_with_uid(app_id, app_certificate, channel_name, uid, Role_Subscriber,
#                                                  token_expiration_in_seconds, privilege_expiration_in_seconds)
#     print("Token with int uid: {}".format(token))
#
# if __name__ == "__main__":
#     main()
