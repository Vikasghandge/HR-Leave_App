provider "aws" {
    region = "ap-south-1"
}

resource "aws_instance" "my-aws_instance" {
    ami = "value"
    instance_type = "t2.meduim"
    
}