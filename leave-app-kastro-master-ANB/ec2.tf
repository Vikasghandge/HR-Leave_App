provider "aws" {
    region = "ap-south-1"
}

resource "aws_instance" "my-aws_instance" {
    ami = "ami-0af9569868786b23a"
    instance_type = "t2.micro"
    
}