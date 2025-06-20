#!/usr/bin/env python3
import os

def rewrite_env(env):
    # *** IMPORTANT: Use 'b' prefix for byte strings! ***
    # Define the old email and the new name/email
    old_email = b"pkp1@kpmg.com" # Exact office email from wrong commits
    correct_name = b"PrajwalMadhyastha" # Your desired personal name
    correct_email = b"prajwalmadhyastha777@gmail.com" # Your desired personal email

    # Check and rewrite author details
    if b"GIT_AUTHOR_EMAIL" in env and env[b"GIT_AUTHOR_EMAIL"] == old_email:
        env[b"GIT_AUTHOR_NAME"] = correct_name
        env[b"GIT_AUTHOR_EMAIL"] = correct_email

    # Check and rewrite committer details
    if b"GIT_COMMITTER_EMAIL" in env and env[b"GIT_COMMITTER_EMAIL"] == old_email:
        env[b"GIT_COMMITTER_NAME"] = correct_name
        env[b"GIT_COMMITTER_EMAIL"] = correct_email

    return env

# This line is crucial for git-filter-repo to find the callback
env_callback = rewrite_env
