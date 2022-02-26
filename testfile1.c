#include <stdio.h>

// TODO: Constant Value Test
/* >>>>>> Constant Value Test <<<<<< */

const int constVar_0 = 0, constVar_1 = 1;
const int constArr1d_0[3] = {1, 2, 3};
const int constArr2d_0[3][3] = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};

/* >>>>>> Constant Value Test End <<<<<< */

// TODO: Variable Test
/* >>>>>> Variable Test <<<<<< */

int var_0;
int var_1 = 1, var_2 = 2, var_3 = 3;
int arr1d_0[3];
int arr1d_1[3] = {1, 2, 3};
int arr1d_2[3] = {4, 5, 6};
int arr1d_3[3] = {7, 8, 9};
int arr2d_0[3][3];
int arr2d_1[3][3] = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};

/* >>>>>> Variable Test End <<<<<< */

// TODO: Function Test
/* >>>>>> Function Test <<<<<< */

// void: nothing

void voidFunc_0 () {

}

void voidFunc_1 (int var_0) {

}

void voidFunc_2 (int arr1d_0[]) {

}

void voidFunc_3 (int arr2d_0[][3]) {

}

void voidFunc_4 (int var_0, int var_1) {

}

void voidFunc_5 (int var_0, int arr1d_0[]) {

}

void voidFunc_6 (int var_0, int arr2d_0[][3]) {

}

void voidFunc_7 (int arr1d_0[], int arr2d_0[][3]) {

}

void voidFunc_8 (int var_0, int arr1d_0[], int arr2d_0[][3]) {

}

// void: semicolon

void voidFunc_9 () {
    ;
}

void voidFunc_10 (int var_0) {
    ;
}

void voidFunc_11 (int arr1d_0[]) {
    ;
}

void voidFunc_12 (int arr2d_0[][3]) {
    ;
}

void voidFunc_13 (int var_0, int var_1) {
    ;
}

void voidFunc_14 (int var_0, int arr1d_0[]) {
    ;
}

void voidFunc_15 (int var_0, int arr2d_0[][3]) {
    ;
}

void voidFunc_16 (int arr1d_0[], int arr2d_0[][3]) {
    ;
}

void voidFunc_17 (int var_0, int arr1d_0[], int arr2d_0[][3]) {
    ;
}

// void: return

void voidFunc_18 () {
    return;
}

void voidFunc_19 (int var_0) {
    return;
}

void voidFunc_20 (int arr1d_0[]) {
    return;
}

void voidFunc_21 (int arr2d_0[][3]) {
    return;
}

void voidFunc_22 (int var_0, int var_1) {
    return;
}

void voidFunc_23 (int var_0, int arr1d_0[]) {
    return;
}

void voidFunc_24 (int var_0, int arr2d_0[][3]) {
    return;
}

void voidFunc_25 (int arr1d_0[], int arr2d_0[][3]) {
    return;
}

void voidFunc_26 (int var_0, int arr1d_0[], int arr2d_0[][3]) {
    return;
}

// int: return own number

int intFunc_0 () {
    return 0;
}

int intFunc_1 (int var_0) {
    return 1;
}

int intFunc_2 (int arr1d_0[]) {
    return 2;
}

int intFunc_3 (int arr2d_0[][3]) {
    return 3;
}

int intFunc_4 (int var_0, int var_1) {
    return 4;
}

int intFunc_5 (int var_0, int arr1d_0[]) {
    return 5;
}

int intFunc_6 (int var_0, int arr2d_0[][3]) {
    return 6;
}

int intFunc_7 (int arr1d_0[], int arr2d_0[][3]) {
    return 7;
}

int intFunc_8 (int var_0, int arr1d_0[], int arr2d_0[][3]) {
    return 8;
}

/* >>>>>> Function Test End <<<<<< */

int getint() {
    int n;
    scanf("%d",&n);
    return n;
}

int main() {
    voidFunc_9();

    // TODO: getint Test
    /* >>>>>> getint Test <<<<<< */

    var_0 = getint();

    /* >>>>>> getint Test End <<<<<< */

    // TODO: Exp Test
    /* >>>>>> Exp Test <<<<<< */
    // blockitem == stmt: LVal = EXP

    var_0 = 0;
    var_0 = +0;
    var_0 = -0;
    var_0 = +-0;
    var_0 = -+0;
    var_0 = +-+0;
    var_0 = -+-0;
    var_0 = var_0 + var_0;
    var_0 = var_0 - var_0;
    var_0 = var_0 * var_0;
    var_0 = (var_0) * 0;
    var_0 = var_0 * intFunc_0();
    var_0 = intFunc_0() * 0;
    var_0 = intFunc_0() * intFunc_8(var_1, arr1d_0, arr2d_1);
    var_0 = var_0 / var_1;
    var_0 = (var_0) / 1;
    var_0 = intFunc_0() / var_1;
    var_0 = intFunc_0() / 1;
    var_0 = intFunc_0() / intFunc_1(arr1d_0[0]);
    var_0 = var_0 % var_2;
    var_0 = (var_0) % 1;
    var_0 = intFunc_0() % var_1;
    var_0 = intFunc_0() % 1;
    var_0 = intFunc_0() % intFunc_1(arr1d_0[0]);
    var_0 = var_1;
    var_0 = (var_1);
    var_0 = arr1d_1[0];
    var_0 = arr2d_1[0][0];

    arr1d_0[0] = 0;
    arr1d_0[0] = var_0;
    arr1d_0[0] = arr1d_1[0];
    arr1d_0[0] = arr2d_1[0][0];

    arr2d_0[0][0] = 0;
    arr2d_0[0][0] = var_0;
    arr2d_0[0][0] = arr1d_1[0];
    arr2d_0[0][0] = arr2d_1[0][0];
    int arr2d_2[3][3] = {{arr2d_1[0][0], arr2d_1[0][1], arr2d_1[0][2]}, {arr2d_1[1][0], arr2d_1[1][1], arr2d_1[1][2]}, {arr2d_1[2][0], arr2d_1[2][1], arr2d_1[2][2]}};

    // /* >>>>>>Exp Test End <<<<<< */

    // TODO: if-else Test
    /* >>>>>> if-else Test <<<<<< */
    if (var_0 == 0) {
        ;
    }

    if (var_1 > var_0) {
        ;
    }
    else if (var_1 >= var_0) {
        ;
    }
    else if (var_1 == var_0) {
        ;
    }
    else if (var_1 != var_0) {
        ;
    }
    else if (var_1 <= var_0) {
        ;
    }
    else if (var_1 < var_0) {
        ;
    }
    else if (var_1 && var_0) {
        ;
    }
    else if (var_1 || var_0) {
        ;
    }
    else {

    }

    /* >>>>>>if-else Test End <<<<<< */

    // TODO: while Test
    /* >>>>>> while Test <<<<<< */

    while (!0) {
        var_0 = var_0 + 1;
        if (var_0 < 10) {
            continue;
        }
        else {
            break;
        }
    }

    /* >>>>>> while Test End <<<<<< */

    printf("%d9373533 ", constVar_1);
    printf("\n19373533");
    printf("\n19373533");
    printf("\n19373533");
    printf("\n19373533");
    printf("\n19373533");
    printf("\n19373533");
    printf("\n19373533");
    printf("\n19373533");
    printf("\n19373533");

    return 0;
}
