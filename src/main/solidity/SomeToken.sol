import "./IERC20.sol";

abstract contract SomeToken is IERC20  {
    uint256 public _liquidityFee;
    uint256 public _maxTxAmount;
    uint256 public _taxFee;
    address public uniswapV2Pair;
}
