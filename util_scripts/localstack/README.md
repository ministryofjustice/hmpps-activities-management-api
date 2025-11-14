
# Localstack/AWS

This folder contains localstack/AWS utility scripts to help with testing the sending/receiving of messages when running the API locally.

Tools required:

* AWS cli

Note: There is an example seed data file containing 3 active allocations for 3 prisoners A11111A, A22222A and A33333A. 

## Config

In `~/.aws/config`:

```
[default]
region = eu-west-2
aws_access_key_id = test
aws_secret_access_key = test
```