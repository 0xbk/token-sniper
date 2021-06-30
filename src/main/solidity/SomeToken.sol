import "./IERC20.sol";

abstract contract SomeToken is IERC20  {
    uint256 public _maxTxAmount;
    address public uniswapV2Pair;
    uint256 public  _burnFee;
    uint256 public  _liquidityFee;
    uint256 public  _marketingFee;
    uint256 public  _potFee;

    // uint256 public sellLimit;
    // bool public tradingEnabled;
    // function getSellLockTimeInSeconds() public virtual view returns(uint256);
}
