package sam.example.accounts

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpStatusCodeException

class NoSuchKey(val id: Long): HttpStatusCodeException(HttpStatus.NOT_FOUND)
