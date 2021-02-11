#include <stdio.h>
#include <stdlib.h>

typedef struct NODE {
  int          value;
  struct NODE *next;
} Node;

typedef Node* (f1)(Node*);
typedef Node* (f2)(Node*, Node*);

Node *cons(int value, Node *node) {
  Node *newNode = malloc(sizeof(Node));
  newNode->next = node;
  newNode->value = value;
  return newNode;
}

Node *reduce(f2 *f, Node *init, Node *coll) {
  return coll 
    ? reduce(f, f(init, coll), coll->next)
    : init;
}

Node *map(f1 *f, Node *x) {
  if (x) {
    Node *y = f(x);
    y->next = map(f, x->next);
    return y;
  } else {
    return x;
  }
}

void show(Node *node) {
  printf("(");
  while (node){
    printf(node->next ? "%d, " : "%d", node->value);
    node = node->next;
  };
  printf(")\n");
}

Node *add(Node *x, Node *y) {
  return cons(x->value + y->value, NULL);
}

Node *inc(Node *x) {
  return add(x, cons(1, NULL));
}

int main () {
  printf("ok\n");
  Node *x = cons(2, cons(1, NULL));
  Node *y = map(inc, x);
  Node *z = reduce(add, cons(0, NULL), y);
  show(x);
  show(y);
  show(z);
  return 0;
}

